package com.krishiai.payment.gateway;

import com.krishiai.payment.entity.Payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentInitiationResult initiatePayment(Payment payment, String successUrl, String failureUrl);

    PaymentVerificationResult verifyPayment(String responsePayload);

    PaymentVerificationResult checkPaymentStatus(String productCode, BigDecimal totalAmount, String transactionUuid);
}
