package com.krishiai.farm.dto;

import com.krishiai.farm.entity.FarmAreaUnit;
import com.krishiai.farm.entity.FarmType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateFarmRequest(
        @Size(max = 150, message = "Farm name must not exceed 150 characters")
        String farmName,

        Long locationId,

        @Positive(message = "Area must be greater than zero")
        BigDecimal area,

        FarmAreaUnit areaUnit,

        FarmType farmType,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
        Double longitude
) {
}
