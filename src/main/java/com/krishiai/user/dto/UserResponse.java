package com.krishiai.user.dto;

import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String profileImage,
        String profileImagePublicId,
        UserRole role,
        UserStatus status,
        boolean emailVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public UserResponse(Long id, String email, String fullName, String phone, String profileImage,
                        UserRole role, UserStatus status, boolean emailVerified, LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        this(id, email, fullName, phone, profileImage, null, role, status, emailVerified, lastLoginAt, createdAt);
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getProfileImage(),
                user.getProfileImagePublicId(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
