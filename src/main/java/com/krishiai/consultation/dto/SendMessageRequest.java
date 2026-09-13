package com.krishiai.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message text cannot be blank")
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        String message
) {
}
