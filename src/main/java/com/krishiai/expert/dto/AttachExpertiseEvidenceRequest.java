package com.krishiai.expert.dto;

import com.krishiai.expert.entity.ExpertiseSourceType;
import jakarta.validation.constraints.NotNull;

public record AttachExpertiseEvidenceRequest(
        @NotNull(message = "Evidence document ID is required")
        Long documentId,

        ExpertiseSourceType sourceType
) {}
