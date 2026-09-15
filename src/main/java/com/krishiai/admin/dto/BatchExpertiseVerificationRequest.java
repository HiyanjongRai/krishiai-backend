package com.krishiai.admin.dto;

import com.krishiai.expert.entity.ExpertiseVerificationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchExpertiseVerificationRequest(
        @NotEmpty(message = "Verification items list cannot be empty")
        @Valid
        List<Item> items,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {
    public record Item(
            @NotNull(message = "Expertise ID is required")
            @Positive(message = "Expertise ID must be positive")
            Long expertiseId,

            @NotNull(message = "Decision is required (VERIFY, REJECT, REQUEST_EVIDENCE)")
            Decision decision,

            @Size(max = 1000, message = "Reason must not exceed 1000 characters")
            String reason,

            ExpertiseVerificationMethod verificationMethod
    ) {}

    public enum Decision {
        VERIFY,
        REJECT,
        REQUEST_EVIDENCE
    }
}
