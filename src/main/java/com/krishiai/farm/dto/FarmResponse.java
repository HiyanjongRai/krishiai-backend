package com.krishiai.farm.dto;

import com.krishiai.farm.entity.Farm;
import com.krishiai.farm.entity.FarmAreaUnit;
import com.krishiai.farm.entity.FarmType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FarmResponse(
        Long id,
        Long farmerId,
        String farmerName,
        String farmName,
        LocationSummary location,
        BigDecimal area,
        FarmAreaUnit areaUnit,
        FarmType farmType,
        String description,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record LocationSummary(
            Long id,
            String name,
            String type
    ) {}

    public static FarmResponse from(Farm farm) {
        LocationSummary locSummary = null;
        if (farm.getLocation() != null) {
            locSummary = new LocationSummary(
                    farm.getLocation().getId(),
                    farm.getLocation().getName(),
                    farm.getLocation().getType() != null ? farm.getLocation().getType().name() : null
            );
        }

        return new FarmResponse(
                farm.getId(),
                farm.getFarmer() != null ? farm.getFarmer().getId() : null,
                farm.getFarmer() != null ? farm.getFarmer().getFullName().strip() : null,
                farm.getFarmName(),
                locSummary,
                farm.getArea(),
                farm.getAreaUnit(),
                farm.getFarmType(),
                farm.getDescription(),
                farm.getLatitude(),
                farm.getLongitude(),
                farm.getCreatedAt(),
                farm.getUpdatedAt()
        );
    }
}
