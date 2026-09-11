package com.krishiai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String fullName,

        @Pattern(
                regexp = "^\\+?[1-9]\\d{6,14}$",
                message = "Phone must be a valid international phone number (E.164 format)"
        )
        String phone,

        String profileImage
) {
}
