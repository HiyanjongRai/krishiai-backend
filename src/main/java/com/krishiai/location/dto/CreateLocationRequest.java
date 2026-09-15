package com.krishiai.location.dto;

import com.krishiai.location.entity.MunicipalityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLocationRequest(
        Long parentId,

        @NotBlank(message = "Location name is required")
        @Size(max = 120, message = "Location name must not exceed 120 characters")
        String name,

        @Size(max = 120, message = "Nepali name must not exceed 120 characters")
        String nepaliName,

        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        MunicipalityType municipalityType,

        Boolean active
) {
}
