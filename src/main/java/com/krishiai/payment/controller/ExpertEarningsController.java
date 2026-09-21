package com.krishiai.payment.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.payment.dto.CreateWithdrawalRequestDto;
import com.krishiai.payment.dto.ExpertEarningsSummaryDto;
import com.krishiai.payment.dto.WithdrawalRequestDto;
import com.krishiai.payment.service.FinancialLedgerService;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing expert earnings summary and withdrawal requests.
 */
@RestController
@RequestMapping("/api/v1/expert")
@PreAuthorize("hasAuthority('ROLE_EXPERT')")
@Validated
@RequiredArgsConstructor
public class ExpertEarningsController {

    private final FinancialLedgerService financialLedgerService;

    /**
     * GET /api/v1/expert/earnings
     * Returns expert earnings breakdown: gross, 5% commission deducted, available, withdrawn.
     */
    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<ExpertEarningsSummaryDto>> getMyEarnings(
            @AuthenticationPrincipal CustomUserDetails principal) {
        ExpertEarningsSummaryDto summary = financialLedgerService.getExpertEarningsSummary(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Earnings retrieved successfully", summary));
    }

    /**
     * GET /api/v1/expert/withdrawals
     * Returns all withdrawal requests submitted by this expert.
     */
    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalRequestDto>>> listWithdrawals(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<WithdrawalRequestDto> withdrawals = financialLedgerService.listMyWithdrawals(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Withdrawals retrieved", withdrawals));
    }

    /**
     * POST /api/v1/expert/withdrawals
     * Expert requests a payout of available earnings to their bank/eSewa/Khalti account.
     */
    @PostMapping("/withdrawals")
    public ResponseEntity<ApiResponse<WithdrawalRequestDto>> requestWithdrawal(
            @Valid @RequestBody CreateWithdrawalRequestDto request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WithdrawalRequestDto dto = financialLedgerService.requestWithdrawal(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal request submitted successfully", dto));
    }
}
