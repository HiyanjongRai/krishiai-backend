package com.krishiai.payment.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.payment.dto.PaymentInitiationResponse;
import com.krishiai.payment.dto.PaymentResponseDto;
import com.krishiai.payment.dto.VerifyPaymentRequest;
import com.krishiai.payment.service.PaymentService;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 * All amount/commission calculations are authoritative on the server – client-supplied amounts are ignored.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@Validated
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments/initiate
     * Farmer-only: initiates eSewa payment for a consultation.
     * Returns signed form fields to POST to eSewa.
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('ROLE_FARMER')")
    public ResponseEntity<ApiResponse<PaymentInitiationResponse>> initiatePayment(
            @RequestParam @Positive Long consultationId,
            @RequestParam(required = false, defaultValue = "http://localhost:3000/payment/callback") String successUrl,
            @RequestParam(required = false, defaultValue = "http://localhost:3000/payment/failure") String failureUrl,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PaymentInitiationResponse response = paymentService.initiateConsultationPayment(
                principal.getUserId(), consultationId, successUrl, failureUrl);
        return ResponseEntity.ok(ApiResponse.success("Payment initiation details generated", response));
    }

    /**
     * POST /api/v1/payments/verify
     * Public-ish: verifies eSewa Base64-encoded callback response.
     * Called by the frontend payment callback page after eSewa redirect.
     * The user must be authenticated (farmer) to verify.
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAuthority('ROLE_FARMER')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        PaymentResponseDto response = paymentService.verifyPayment(request.getData());
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", response));
    }

    /**
     * GET /api/v1/payments/{id}/status
     * Farmer or Expert: check status of a payment.
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_FARMER', 'ROLE_EXPERT', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentStatus(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PaymentResponseDto response = paymentService.getPaymentStatus(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", response));
    }
}
