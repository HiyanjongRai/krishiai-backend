package com.krishiai.expert.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateInquiryRequest(
        @NotBlank(message = "Status cannot be blank")
        String status,
        String expertNotes
) {}
