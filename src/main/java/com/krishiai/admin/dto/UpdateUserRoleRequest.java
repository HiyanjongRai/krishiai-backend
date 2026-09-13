package com.krishiai.admin.dto;

import com.krishiai.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        UserRole role,

        String reason
) {
}
