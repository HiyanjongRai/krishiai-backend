package com.krishiai.admin.dto;

import java.util.List;

public record AdminDashboardStatsResponse(
        long totalFarmers,
        long activeFarmers,
        long newFarmersThisMonth,
        double farmerGrowthRate,
        long totalExperts,
        long verifiedExperts,
        long pendingVerifications,
        long suspendedExperts,
        long cropAnalyses,
        long aiReviews,
        double platformAccuracy,
        boolean allSystemsOperational,
        int highConfidenceRate,
        int mediumConfidenceRate,
        int lowConfidenceRate,
        long aiConfirmedCount,
        long aiCorrectedCount,
        long aiMoreInfoCount,
        double aiCorrectionRate,
        int aiPendingReviewsCount,
        int knowledgeArticlesPendingCount,
        List<GrowthPointDto> growthPoints,
        List<CropStatDto> analyzedCrops,
        List<ConditionStatDto> detectedConditions,
        List<ActivityDto> recentActivities,
        List<HealthStatusDto> systemHealth
) {
    public record GrowthPointDto(
            String dateLabel,
            int farmers,
            int experts,
            int activeUsers
    ) {}

    public record CropStatDto(
            String name,
            String emoji,
            long analyses,
            double percentage,
            String trend
    ) {}

    public record ConditionStatDto(
            String condition,
            String crop,
            long detections,
            String severity,
            int expertConfirmationRate
    ) {}

    public record ActivityDto(
            String title,
            String description,
            String timeAgo,
            String type
    ) {}

    public record HealthStatusDto(
            String service,
            String status,
            boolean operational
    ) {}
}

