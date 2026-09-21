package com.krishiai.payment.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiationResult {
    private String paymentUrl;
    private String method; // "POST"
    private Map<String, String> formFields;
    private String transactionUuid;
}
