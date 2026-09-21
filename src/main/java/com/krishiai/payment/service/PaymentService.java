package com.krishiai.payment.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.consultation.entity.Consultation;
import com.krishiai.consultation.entity.ConsultationStatus;
import com.krishiai.consultation.repository.ConsultationRepository;
import com.krishiai.messaging.service.ConsultationLifecycleService;
import com.krishiai.payment.dto.PaymentInitiationResponse;
import com.krishiai.payment.dto.PaymentResponseDto;
import com.krishiai.payment.entity.Payment;
import com.krishiai.payment.entity.PaymentProvider;
import com.krishiai.payment.entity.PaymentStatus;
import com.krishiai.payment.gateway.PaymentGateway;
import com.krishiai.payment.gateway.PaymentInitiationResult;
import com.krishiai.payment.gateway.PaymentVerificationResult;
import com.krishiai.payment.repository.PaymentRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final CommissionService commissionService;
    private final FinancialLedgerService financialLedgerService;
    @Lazy
    private final ConsultationLifecycleService consultationLifecycleService;

    /**
     * Initiates payment for a consultation.
     * Enforces that only the consultation owner (farmer) can pay,
     * the price is authoritatively derived from the database (never from the client),
     * and generates a unique transaction UUID with HMAC-SHA256 signature for eSewa.
     */
    @Transactional
    public PaymentInitiationResponse initiateConsultationPayment(Long farmerId, Long consultationId, String successUrl, String failureUrl) {
        Consultation consultation = consultationRepository.findByIdWithDetails(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with id: " + consultationId));

        if (!consultation.getFarmer().getId().equals(farmerId)) {
            throw new ForbiddenException("You are not authorized to pay for this consultation");
        }

        if (consultation.getStatus() != ConsultationStatus.PAYMENT_PENDING && consultation.getStatus() != ConsultationStatus.REQUESTED) {
            throw new BadRequestException("Consultation is not awaiting payment. Current status: " + consultation.getStatus());
        }

        BigDecimal amount = consultation.getPriceAtPurchase();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Consultation has invalid or zero fee");
        }

        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        // Calculate platform commission and expert amount
        CommissionService.CommissionBreakdown breakdown = commissionService.calculateCommission(amount);

        // Generate unique transaction UUID (alphanumeric and hyphen only)
        String shortRandom = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String transactionUuid = String.format("KAI-%d-%d-%s", consultationId, System.currentTimeMillis(), shortRandom);

        // Create pending payment record
        Payment payment = new Payment(
                consultation,
                farmer,
                PaymentProvider.ESEWA,
                transactionUuid,
                breakdown.grossAmount(),
                breakdown.platformCommission(),
                breakdown.expertAmount()
        );
        payment = paymentRepository.save(payment);

        // Generate signed payment parameters via eSewa gateway
        PaymentInitiationResult initResult = paymentGateway.initiatePayment(payment, successUrl, failureUrl);

        log.info("Payment initiated: paymentId={}, transactionUuid={}, amount={}, farmerId={}",
                payment.getId(), transactionUuid, amount, farmerId);

        return PaymentInitiationResponse.builder()
                .paymentId(payment.getId())
                .consultationId(consultation.getId())
                .transactionUuid(transactionUuid)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentUrl(initResult.getPaymentUrl())
                .method(initResult.getMethod())
                .formFields(initResult.getFormFields())
                .build();
    }

    /**
     * Verifies payment server-side using eSewa's HMAC-SHA256 signature and status check API.
     * IDEMPOTENT: repeated verification calls will not double-credit earnings or duplicate records.
     * ANTI-TAMPERING: verifies that the exact amount confirmed by eSewa matches our database record.
     */
    @Transactional
    public PaymentResponseDto verifyPayment(String encodedData) {
        if (encodedData == null || encodedData.isBlank()) {
            throw new BadRequestException("Payment response data is required");
        }

        // 1. Verify eSewa signature and decode response
        PaymentVerificationResult gatewayResult = paymentGateway.verifyPayment(encodedData);
        if (!gatewayResult.isVerified()) {
            log.warn("eSewa signature verification failed: reason={}", gatewayResult.getFailureReason());
            throw new BadRequestException("Payment signature verification failed: " + gatewayResult.getFailureReason());
        }

        String transactionUuid = gatewayResult.getTransactionUuid();
        Payment payment = paymentRepository.findByTransactionUuidWithDetails(transactionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for transaction: " + transactionUuid));

        // 2. IDEMPOTENCY: If payment is already marked SUCCESS, return existing record
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already verified (idempotent replay): transactionUuid={}", transactionUuid);
            return PaymentResponseDto.fromEntity(payment);
        }

        // 3. CRITICAL ANTI-TAMPERING CHECK: Compare eSewa amount with KrishiAI stored amount
        BigDecimal expectedAmount = payment.getAmount();
        BigDecimal receivedAmount = gatewayResult.getTotalAmount();
        if (expectedAmount.compareTo(receivedAmount) != 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.error("SECURITY ALERT: Payment amount mismatch! expected={}, received={}, transactionUuid={}",
                    expectedAmount, receivedAmount, transactionUuid);
            throw new BadRequestException(String.format("Payment amount mismatch! Expected NPR %s but received NPR %s",
                    expectedAmount, receivedAmount));
        }

        // 4. Server-to-server status inquiry to confirm completion
        PaymentVerificationResult statusCheck = paymentGateway.checkPaymentStatus(
                gatewayResult.getProductCode(),
                expectedAmount,
                transactionUuid
        );

        if (!statusCheck.isVerified()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.warn("eSewa status check failed for uuid={}: reason={}", transactionUuid, statusCheck.getFailureReason());
            throw new BadRequestException("eSewa transaction status check failed: " + statusCheck.getFailureReason());
        }

        // 5. Update Payment to SUCCESS
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderTransactionId(gatewayResult.getProviderTransactionId());
        payment.setProviderReferenceId(statusCheck.getProviderReferenceId() != null
                ? statusCheck.getProviderReferenceId()
                : gatewayResult.getProviderTransactionId());
        payment.setPaidAt(LocalDateTime.now());
        payment.setRawResponse(gatewayResult.getRawResponse());
        payment = paymentRepository.save(payment);

        // 6. Record financial ledger entries (Platform commission & Expert earnings)
        financialLedgerService.recordPaymentSuccess(payment);

        // 7. Activate consultation & unlock messaging
        consultationLifecycleService.activatePaidConsultation(payment);

        log.info("Payment SUCCESS confirmed: id={}, uuid={}, amount={}", payment.getId(), transactionUuid, payment.getAmount());

        return PaymentResponseDto.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentStatus(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        boolean isPayer = payment.getPayer().getId().equals(userId);
        boolean isExpert = payment.getConsultation().getExpert() != null && payment.getConsultation().getExpert().getId().equals(userId);

        if (!isPayer && !isExpert) {
            throw new ForbiddenException("You are not authorized to view this payment");
        }

        return PaymentResponseDto.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> listPaymentsForConsultation(Long consultationId, Long userId) {
        return paymentRepository.findByConsultationIdOrderByCreatedAtDesc(consultationId)
                .stream()
                .map(PaymentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}
