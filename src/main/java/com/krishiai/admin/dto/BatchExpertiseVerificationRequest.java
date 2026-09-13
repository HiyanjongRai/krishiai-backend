package com.krishiai.admin.dto;

import com.krishiai.expert.entity.ExpertiseVerificationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchExpertiseVerificationRequest(
        @NotEmpty(message = "Verification items list cannot be empty")
        @Valid
        List<Item> items,

        String notes
) {
    public record Item(
            @NotNull(message = "Expertise ID is required")
            Long expertiseId,

            @NotNull(message = "Decision is required (VERIFY, REJECT, REQUEST_EVIDENCE)")
            String decision,

            String reason,

            ExpertiseVerificationMethod verificationMethod
    ) {}
}
