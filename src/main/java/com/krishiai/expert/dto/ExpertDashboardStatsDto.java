package com.krishiai.expert.dto;

public record ExpertDashboardStatsDto(
        int totalConsultations,
        int pendingInquiries,
        int activeCases,
        int totalFarmersAssisted,
        int assignedCropsCount,
        int specializationsCount,
        int locationsCount,
        int profileCompletionPercentage,
        double averageRating,
        int totalReviews,
        String applicationStatus,
        boolean verifiedExpert,
        double responseTimeHours
) {}
