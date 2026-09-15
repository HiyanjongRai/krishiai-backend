package com.krishiai.admin.dto;

import com.krishiai.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a user's account status (block, unblock, suspend, etc.).
 */
public record UpdateUserStatusRequest(
        @NotNull(message = "User status must not be null")
        UserStatus status,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
