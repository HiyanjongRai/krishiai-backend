package com.krishiai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiationResponse {
    private Long paymentId;
    private Long consultationId;
    private String transactionUuid;
    private BigDecimal amount;
    private String currency;
    private String paymentUrl;
    private String method;
    private Map<String, String> formFields;
}
