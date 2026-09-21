package com.krishiai.payment.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationResult {
    private boolean verified;
    private String transactionUuid;
    private String providerTransactionId; // e.g. transaction_code
    private String providerReferenceId; // e.g. ref_id
    private BigDecimal totalAmount;
    private String productCode;
    private String status; // "COMPLETE", etc.
    private String rawResponse;
    private String failureReason;
}
