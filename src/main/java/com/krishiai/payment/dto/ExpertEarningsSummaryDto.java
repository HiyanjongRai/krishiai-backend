package com.krishiai.payment.dto;

import com.krishiai.payment.entity.WalletLedgerEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpertEarningsSummaryDto {
    private BigDecimal totalEarnings;
    private BigDecimal availableEarnings;
    private BigDecimal pendingWithdrawal;
    private BigDecimal totalWithdrawn;
    private List<WalletLedgerEntry> transactions;
}
