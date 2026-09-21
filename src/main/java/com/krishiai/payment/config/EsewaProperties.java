package com.krishiai.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "esewa")
public class EsewaProperties {

    /** "test" or "production" */
    private String environment = "test";

    /** Merchant Product Code (EPAYTEST for development) */
    private String productCode = "EPAYTEST";

    /** Secret Key for HMAC-SHA256 signature (test default: 8gBm/:&EnhH.1/q) */
    private String secretKey = "8gBm/:&EnhH.1/q";

    /** Frontend callback URL on successful payment */
    private String successUrl = "http://localhost:3000/payment/callback";

    /** Frontend callback URL on failed payment */
    private String failureUrl = "http://localhost:3000/payment/callback";

    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

    public String getPaymentFormUrl() {
        return isProduction()
                ? "https://epay.esewa.com.np/api/epay/main/v2/form"
                : "https://rc-epay.esewa.com.np/api/epay/main/v2/form";
    }

    public String getStatusCheckUrl() {
        return isProduction()
                ? "https://epay.esewa.com.np/api/epay/transaction/status/"
                : "https://rc.esewa.com.np/api/epay/transaction/status/";
    }
}
