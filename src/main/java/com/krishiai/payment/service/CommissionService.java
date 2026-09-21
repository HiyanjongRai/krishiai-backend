package com.krishiai.payment.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.payment.config.PlatformPricingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private final PlatformPricingProperties properties;

    public record CommissionBreakdown(
            BigDecimal grossAmount,
            BigDecimal commissionRate,
            BigDecimal platformCommission,
            BigDecimal expertAmount
    ) {}

    /**
     * Authoritative calculation of platform commission and expert earnings.
     * Uses BigDecimal with RoundingMode.HALF_UP and scale 2.
     */
    public CommissionBreakdown calculateCommission(BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Gross amount must be greater than zero");
        }

        BigDecimal normalizedGross = grossAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionRate = properties.getCommissionPercentage();

        // platformCommission = grossAmount * (commissionPercentage / 100)
        BigDecimal commission = normalizedGross
                .multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // expertAmount = grossAmount - platformCommission
        BigDecimal expertAmount = normalizedGross.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        return new CommissionBreakdown(normalizedGross, commissionRate, commission, expertAmount);
    }

    /**
     * Validates package fee and duration according to platform rules.
     */
    public void validatePackage(BigDecimal price, Integer durationHours) {
        if (price == null || price.compareTo(properties.getMinimumFee()) < 0) {
            throw new BadRequestException("Package fee cannot be less than NPR " + properties.getMinimumFee());
        }

        if (price.compareTo(properties.getMaximumFee()) > 0) {
            throw new BadRequestException("Package fee cannot exceed NPR " + properties.getMaximumFee());
        }

        if (durationHours == null || !properties.getAllowedDurations().contains(durationHours)) {
            throw new BadRequestException("Duration " + durationHours + " hours is not permitted. Allowed durations (hours): "
                    + properties.getAllowedDurations());
        }
    }
}
