package com.krishiai.farm.dto;

import com.krishiai.user.dto.UserResponse;

import java.util.List;

public record FarmerDashboardResponse(
        UserResponse farmer,
        FarmResponse primaryFarm,
        int totalFarms,
        long cropCount,
        long activeConsultations,
        List<RecentActivityDto> recentActivity,
        List<DashboardNotificationDto> notifications
) {
    public record RecentActivityDto(
            String title,
            String description,
            String timeAgo,
            String type
    ) {}

    public record DashboardNotificationDto(
            String id,
            String message,
            String type,
            boolean read
    ) {}
}
