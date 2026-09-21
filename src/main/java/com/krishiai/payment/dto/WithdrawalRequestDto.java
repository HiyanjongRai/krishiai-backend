package com.krishiai.payment.dto;

import com.krishiai.payment.entity.WithdrawalRequest;
import com.krishiai.payment.entity.WithdrawalStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class WithdrawalRequestDto {
    private Long id;
    private Long expertId;
    private String expertName;
    private BigDecimal amount;
    private String currency;
    private WithdrawalStatus status;
    private String accountDetails;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String adminNotes;

    public static WithdrawalRequestDto fromEntity(WithdrawalRequest req) {
        if (req == null) return null;
        String expertName = req.getExpert() != null ? req.getExpert().getFullName() : "Unknown Expert";
        return WithdrawalRequestDto.builder()
                .id(req.getId())
                .expertId(req.getExpert() != null ? req.getExpert().getId() : null)
                .expertName(expertName)
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .status(req.getStatus())
                .accountDetails(req.getAccountDetails())
                .requestedAt(req.getRequestedAt())
                .processedAt(req.getProcessedAt())
                .adminNotes(req.getAdminNotes())
                .build();
    }
}
