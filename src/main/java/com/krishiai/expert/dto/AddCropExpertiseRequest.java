package com.krishiai.expert.dto;

import com.krishiai.expert.entity.CropExpertiseType;
import com.krishiai.expert.entity.ExpertiseLevel;
import com.krishiai.expert.entity.ExpertiseSourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body to add or update an expertise claim (crop or agricultural domain).
 */
public record AddCropExpertiseRequest(
        @Positive(message = "Crop ID must be positive")
        Long cropId,

        @Size(max = 120, message = "Expertise area name cannot exceed 120 characters")
        String expertiseArea,

        CropExpertiseType expertiseType,

        ExpertiseLevel expertiseLevel,

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 80, message = "Years of experience must be realistic")
        Integer yearsOfExperience,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        ExpertiseSourceType sourceType,

        @Positive(message = "Evidence document ID must be positive")
        Long evidenceDocumentId
) {}
