package com.krishiai.payment.dto;

import com.krishiai.payment.entity.Payment;
import com.krishiai.payment.entity.PaymentProvider;
import com.krishiai.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private Long consultationId;
    private Long payerId;
    private PaymentProvider provider;
    private String transactionUuid;
    private String providerTransactionId;
    private String providerReferenceId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal platformCommission;
    private BigDecimal expertAmount;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static PaymentResponseDto fromEntity(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .consultationId(payment.getConsultation().getId())
                .payerId(payment.getPayer().getId())
                .provider(payment.getProvider())
                .transactionUuid(payment.getTransactionUuid())
                .providerTransactionId(payment.getProviderTransactionId())
                .providerReferenceId(payment.getProviderReferenceId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .platformCommission(payment.getPlatformCommission())
                .expertAmount(payment.getExpertAmount())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
