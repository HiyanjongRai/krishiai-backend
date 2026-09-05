package com.krishiai.expert.dto;

import com.krishiai.expert.entity.CropExpertiseType;
import jakarta.validation.constraints.NotNull;

/**
 * Request body to add or update a crop expertise entry.
 */
public record AddCropExpertiseRequest(
        @NotNull(message = "Crop ID is required")
        Long cropId,

        @NotNull(message = "Expertise type is required (PRIMARY or SECONDARY)")
        CropExpertiseType expertiseType
) {}
