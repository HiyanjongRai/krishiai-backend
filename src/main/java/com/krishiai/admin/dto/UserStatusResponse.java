package com.krishiai.admin.dto;

import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;

import java.time.LocalDateTime;

/**
 * Response DTO returned after an admin updates a user's status.
 */
public record UserStatusResponse(
        Long userId,
        String email,
        String fullName,
        UserRole role,
        UserStatus status,
        LocalDateTime updatedAt
) {
    public static UserStatusResponse from(User user) {
        return new UserStatusResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getUpdatedAt()
        );
    }
}
