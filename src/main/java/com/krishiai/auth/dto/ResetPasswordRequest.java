package com.krishiai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token must not be blank")
        String token,

        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "New password must contain at least one letter and one number"
        )
        String newPassword,

        @NotBlank(message = "Confirm password must not be blank")
        String confirmPassword
) {
}
