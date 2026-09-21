package com.krishiai.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "krishiai.platform")
public class PlatformPricingProperties {

    /** Platform commission percentage (e.g. 5.0 for 5%). */
    private BigDecimal commissionPercentage = new BigDecimal("5.0");

    /** Minimum allowable consultation package fee. */
    private BigDecimal minimumFee = new BigDecimal("100.00");

    /** Maximum allowable consultation package fee. */
    private BigDecimal maximumFee = new BigDecimal("10000.00");

    /** Allowed consultation duration hours (e.g. 24 = 1 day, 72 = 3 days, 168 = 7 days, 336 = 14 days, 720 = 30 days). */
    private List<Integer> allowedDurations = List.of(24, 72, 168, 336, 720);
}
