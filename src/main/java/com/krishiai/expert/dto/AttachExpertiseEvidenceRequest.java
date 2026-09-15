package com.krishiai.expert.dto;

import com.krishiai.expert.entity.ExpertiseSourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttachExpertiseEvidenceRequest(
        @NotNull(message = "Evidence document ID is required")
        @Positive(message = "Evidence document ID must be positive")
        Long documentId,

        ExpertiseSourceType sourceType
) {}
