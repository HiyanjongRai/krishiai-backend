package com.krishiai.payment.gateway.esewa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishiai.common.exception.BadRequestException;
import com.krishiai.payment.config.EsewaProperties;
import com.krishiai.payment.entity.Payment;
import com.krishiai.payment.gateway.PaymentGateway;
import com.krishiai.payment.gateway.PaymentInitiationResult;
import com.krishiai.payment.gateway.PaymentVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsewaPaymentGateway implements PaymentGateway {

    private final EsewaProperties esewaProperties;

    /** Instantiated directly — Jackson ObjectMapper is stateless and safe to reuse. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Override
    public PaymentInitiationResult initiatePayment(Payment payment, String successUrl, String failureUrl) {
        String totalAmountStr = formatAmount(payment.getAmount());
        String transactionUuid = payment.getTransactionUuid();
        String productCode = esewaProperties.getProductCode();

        // 1. Construct message to sign: "total_amount=X,transaction_uuid=Y,product_code=Z"
        String message = String.format("total_amount=%s,transaction_uuid=%s,product_code=%s",
                totalAmountStr, transactionUuid, productCode);

        // 2. Generate HMAC-SHA256 signature
        String signature = generateHmacSha256(message, esewaProperties.getSecretKey());

        // 3. Build signed form parameters
        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("amount", totalAmountStr);
        formFields.put("tax_amount", "0");
        formFields.put("total_amount", totalAmountStr);
        formFields.put("transaction_uuid", transactionUuid);
        formFields.put("product_code", productCode);
        formFields.put("product_service_charge", "0");
        formFields.put("product_delivery_charge", "0");
        formFields.put("success_url", successUrl != null && !successUrl.isBlank() ? successUrl : esewaProperties.getSuccessUrl());
        formFields.put("failure_url", failureUrl != null && !failureUrl.isBlank() ? failureUrl : esewaProperties.getFailureUrl());
        formFields.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        formFields.put("signature", signature);

        log.info("eSewa payment initiated: transactionUuid={}, amount={}", transactionUuid, totalAmountStr);

        return PaymentInitiationResult.builder()
                .paymentUrl(esewaProperties.getPaymentFormUrl())
                .method("POST")
                .formFields(formFields)
                .transactionUuid(transactionUuid)
                .build();
    }

    @Override
    public PaymentVerificationResult verifyPayment(String responsePayload) {
        try {
            // Decodes Base64 data if needed
            String jsonContent = responsePayload;
            if (!responsePayload.trim().startsWith("{")) {
                byte[] decodedBytes = Base64.getDecoder().decode(responsePayload.trim());
                jsonContent = new String(decodedBytes, StandardCharsets.UTF_8);
            }

            JsonNode root = objectMapper.readTree(jsonContent);

            String transactionCode = root.path("transaction_code").asText("");
            String status = root.path("status").asText("");
            String totalAmountStr = root.path("total_amount").asText("");
            String transactionUuid = root.path("transaction_uuid").asText("");
            String productCode = root.path("product_code").asText("");
            String signedFieldNames = root.path("signed_field_names").asText("");
            String signature = root.path("signature").asText("");

            BigDecimal totalAmount = new BigDecimal(totalAmountStr.replace(",", ""));

            // Verify signature against signed_field_names
            boolean signatureValid = false;
            if (!signedFieldNames.isBlank() && !signature.isBlank()) {
                String[] fields = signedFieldNames.split(",");
                List<String> parts = new ArrayList<>();
                for (String field : fields) {
                    field = field.trim();
                    if (root.has(field)) {
                        parts.add(field + "=" + root.get(field).asText());
                    }
                }
                String messageToSign = String.join(",", parts);
                String expectedSignature = generateHmacSha256(messageToSign, esewaProperties.getSecretKey());

                // Constant-time signature verification to prevent timing attacks
                signatureValid = MessageDigest.isEqual(
                        signature.getBytes(StandardCharsets.UTF_8),
                        expectedSignature.getBytes(StandardCharsets.UTF_8)
                );
            }

            boolean isComplete = "COMPLETE".equalsIgnoreCase(status);

            log.info("eSewa callback verification: uuid={}, status={}, signatureValid={}",
                    transactionUuid, status, signatureValid);

            return PaymentVerificationResult.builder()
                    .verified(signatureValid && isComplete)
                    .transactionUuid(transactionUuid)
                    .providerTransactionId(transactionCode)
                    .totalAmount(totalAmount)
                    .productCode(productCode)
                    .status(status)
                    .rawResponse(jsonContent)
                    .failureReason(!signatureValid ? "Invalid HMAC signature" : (!isComplete ? "Status is not COMPLETE" : null))
                    .build();

        } catch (Exception e) {
            log.error("Failed to verify eSewa payment response", e);
            return PaymentVerificationResult.builder()
                    .verified(false)
                    .failureReason("Parsing/Verification error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentVerificationResult checkPaymentStatus(String productCode, BigDecimal totalAmount, String transactionUuid) {
        try {
            String totalAmountStr = formatAmount(totalAmount);
            String url = String.format("%s?product_code=%s&total_amount=%s&transaction_uuid=%s",
                    esewaProperties.getStatusCheckUrl(),
                    productCode,
                    totalAmountStr,
                    transactionUuid);

            log.info("Querying eSewa server-to-server status check: url={}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return PaymentVerificationResult.builder()
                        .verified(false)
                        .transactionUuid(transactionUuid)
                        .failureReason("Status check API returned status " + response.getStatusCode())
                        .build();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("status").asText("");
            String refId = root.path("ref_id").asText("");
            String resAmountStr = root.path("total_amount").asText("");

            BigDecimal verifiedAmount = resAmountStr.isBlank() ? totalAmount : new BigDecimal(resAmountStr);
            boolean isComplete = "COMPLETE".equalsIgnoreCase(status);

            return PaymentVerificationResult.builder()
                    .verified(isComplete)
                    .transactionUuid(transactionUuid)
                    .providerTransactionId(refId)
                    .providerReferenceId(refId)
                    .totalAmount(verifiedAmount)
                    .productCode(productCode)
                    .status(status)
                    .rawResponse(response.getBody())
                    .failureReason(isComplete ? null : "Status is " + status)
                    .build();

        } catch (Exception e) {
            log.error("eSewa status check API exception for uuid={}", transactionUuid, e);
            return PaymentVerificationResult.builder()
                    .verified(false)
                    .transactionUuid(transactionUuid)
                    .failureReason("Status check exception: " + e.getMessage())
                    .build();
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        BigDecimal stripped = amount.stripTrailingZeros();
        // If whole number, format without decimal point per eSewa examples (e.g. 500)
        if (stripped.scale() <= 0) {
            return stripped.toBigInteger().toString();
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String generateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate HMAC-SHA256 signature: " + e.getMessage());
        }
    }
}
