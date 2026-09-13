package com.krishiai.admin.dto;

import com.krishiai.user.entity.User;
import java.time.LocalDateTime;

public record FarmerSummaryResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String status,
        boolean emailVerified,
        String profileImage,
        LocalDateTime createdAt
) {
    public static FarmerSummaryResponse from(User user) {
        return new FarmerSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
                user.isEmailVerified(),
                user.getProfileImage(),
                user.getCreatedAt()
        );
    }
}
