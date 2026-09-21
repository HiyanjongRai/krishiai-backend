package com.krishiai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminFinancialSummaryDto {
    private BigDecimal totalGrossRevenue;
    private BigDecimal totalPlatformCommission;
    private BigDecimal totalExpertEarnings;
    private long successfulPaymentsCount;
    private long pendingPaymentsCount;
    private long failedPaymentsCount;
}
