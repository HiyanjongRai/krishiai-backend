package com.krishiai.payment.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.payment.dto.AdminFinancialSummaryDto;
import com.krishiai.payment.dto.PaymentResponseDto;
import com.krishiai.payment.dto.WithdrawalRequestDto;
import com.krishiai.payment.repository.PaymentRepository;
import com.krishiai.payment.service.FinancialLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-only REST controller for platform financial oversight.
 * All endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Validated
@RequiredArgsConstructor
public class AdminFinancialController {

    private final FinancialLedgerService financialLedgerService;
    private final PaymentRepository paymentRepository;

    /**
     * GET /api/v1/admin/financials
     * Returns platform-wide revenue summary: gross, commission, expert earnings, counts.
     */
    @GetMapping("/financials")
    public ResponseEntity<ApiResponse<AdminFinancialSummaryDto>> getFinancialSummary() {
        AdminFinancialSummaryDto summary = financialLedgerService.getAdminFinancialSummary();
        return ResponseEntity.ok(ApiResponse.success("Financial summary retrieved", summary));
    }

    /**
     * GET /api/v1/admin/payments
     * Returns paginated list of all payment transactions for audit purposes.
     */
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        List<PaymentResponseDto> payments = paymentRepository
                .findAll(PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(PaymentResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", payments));
    }

    /**
     * GET /api/v1/admin/withdrawals/pending
     * Returns all pending withdrawal requests awaiting admin approval.
     */
    @GetMapping("/withdrawals/pending")
    public ResponseEntity<ApiResponse<List<WithdrawalRequestDto>>> getPendingWithdrawals() {
        List<WithdrawalRequestDto> withdrawals = financialLedgerService.listPendingWithdrawals();
        return ResponseEntity.ok(ApiResponse.success("Pending withdrawals retrieved", withdrawals));
    }
}
