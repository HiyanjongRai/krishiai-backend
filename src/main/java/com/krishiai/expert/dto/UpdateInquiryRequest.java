package com.krishiai.expert.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateInquiryRequest(
        @NotNull(message = "Status is required")
        InquiryStatus status,

        @Size(max = 2000, message = "Expert notes cannot exceed 2000 characters")
        String expertNotes
) {
    public enum InquiryStatus {
        PENDING_REVIEW,
        IN_PROGRESS,
        RESOLVED
    }
}
