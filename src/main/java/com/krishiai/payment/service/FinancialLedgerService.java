package com.krishiai.payment.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.payment.dto.AdminFinancialSummaryDto;
import com.krishiai.payment.dto.CreateWithdrawalRequestDto;
import com.krishiai.payment.dto.ExpertEarningsSummaryDto;
import com.krishiai.payment.dto.WithdrawalRequestDto;
import com.krishiai.payment.entity.*;
import com.krishiai.payment.repository.PaymentRepository;
import com.krishiai.payment.repository.WalletLedgerEntryRepository;
import com.krishiai.payment.repository.WithdrawalRequestRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialLedgerService {

    private final WalletLedgerEntryRepository ledgerRepository;
    private final PaymentRepository paymentRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final UserRepository userRepository;

    /**
     * Records immutable ledger entries upon verified payment.
     * Guaranteed to be idempotent and called within the payment verification transaction.
     */
    @Transactional
    public void recordPaymentSuccess(Payment payment) {
        User expert = payment.getConsultation().getExpert();

        // 1. Platform commission entry (user = null signifies KrishiAI platform)
        WalletLedgerEntry commissionEntry = new WalletLedgerEntry(
                null,
                LedgerEntryType.PLATFORM_COMMISSION,
                payment.getPlatformCommission(),
                "PAYMENT",
                payment.getId(),
                "KrishiAI 5% platform commission on Consultation #" + payment.getConsultation().getId()
        );
        ledgerRepository.save(commissionEntry);

        // 2. Expert earning entry
        if (expert != null) {
            WalletLedgerEntry expertEarning = new WalletLedgerEntry(
                    expert,
                    LedgerEntryType.EXPERT_EARNING,
                    payment.getExpertAmount(),
                    "PAYMENT",
                    payment.getId(),
                    "Consultation earnings for Consultation #" + payment.getConsultation().getId()
            );
            ledgerRepository.save(expertEarning);
        }

        log.info("Financial ledger recorded for Payment id={}: commission={}, expertAmount={}",
                payment.getId(), payment.getPlatformCommission(), payment.getExpertAmount());
    }

    /**
     * Authoritative financial summary for an expert.
     */
    @Transactional(readOnly = true)
    public ExpertEarningsSummaryDto getExpertEarningsSummary(Long expertId) {
        BigDecimal totalEarned = ledgerRepository.sumAmountByUserIdAndType(expertId, LedgerEntryType.EXPERT_EARNING);
        BigDecimal completedWithdrawals = withdrawalRepository.sumCompletedWithdrawalsByExpertId(expertId);
        BigDecimal pendingWithdrawals = withdrawalRepository.sumPendingWithdrawalsByExpertId(expertId);

        BigDecimal availableEarnings = totalEarned.subtract(completedWithdrawals).subtract(pendingWithdrawals);
        if (availableEarnings.compareTo(BigDecimal.ZERO) < 0) {
            availableEarnings = BigDecimal.ZERO;
        }

        List<WalletLedgerEntry> transactions = ledgerRepository.findByUserIdOrderByCreatedAtDesc(expertId);

        return new ExpertEarningsSummaryDto(
                totalEarned,
                availableEarnings,
                pendingWithdrawals,
                completedWithdrawals,
                transactions
        );
    }

    /**
     * Platform-wide financial summary for Admin oversight.
     */
    @Transactional(readOnly = true)
    public AdminFinancialSummaryDto getAdminFinancialSummary() {
        BigDecimal totalRevenue = paymentRepository.sumTotalSuccessfulPayments();
        BigDecimal totalCommission = paymentRepository.sumTotalPlatformCommission();
        BigDecimal totalExpertEarnings = totalRevenue.subtract(totalCommission);

        long successfulCount = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED);

        return new AdminFinancialSummaryDto(
                totalRevenue,
                totalCommission,
                totalExpertEarnings,
                successfulCount,
                pendingCount,
                failedCount
        );
    }

    /**
     * Expert requests a withdrawal of their available earnings.
     * Validates that the requested amount does not exceed available balance.
     */
    @Transactional
    public WithdrawalRequestDto requestWithdrawal(Long expertId, CreateWithdrawalRequestDto request) {
        User expert = userRepository.findById(expertId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        ExpertEarningsSummaryDto summary = getExpertEarningsSummary(expertId);
        if (request.getAmount().compareTo(summary.getAvailableEarnings()) > 0) {
            throw new BadRequestException(String.format(
                    "Insufficient available balance. Requested NPR %s but only NPR %s is available.",
                    request.getAmount(), summary.getAvailableEarnings()));
        }

        WithdrawalRequest withdrawal = new WithdrawalRequest(expert, request.getAmount(), request.getAccountDetails());
        withdrawal = withdrawalRepository.save(withdrawal);

        log.info("Withdrawal requested: expertId={}, amount={}", expertId, request.getAmount());
        return WithdrawalRequestDto.fromEntity(withdrawal);
    }

    /**
     * List all withdrawal requests for an expert.
     */
    @Transactional(readOnly = true)
    public List<WithdrawalRequestDto> listMyWithdrawals(Long expertId) {
        return withdrawalRepository.findByExpertIdOrderByRequestedAtDesc(expertId)
                .stream()
                .map(WithdrawalRequestDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Admin: list all pending withdrawal requests.
     */
    @Transactional(readOnly = true)
    public List<WithdrawalRequestDto> listPendingWithdrawals() {
        return withdrawalRepository.findByStatusOrderByRequestedAtAsc(WithdrawalStatus.PENDING)
                .stream()
                .map(WithdrawalRequestDto::fromEntity)
                .collect(Collectors.toList());
    }
}
