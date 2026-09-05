package com.krishiai.expert.dto;

import java.util.List;

public record ExpertDashboardResponse(
        ExpertProfileResponse profile,
        ExpertDashboardStatsDto stats,
        List<FarmerInquiryDto> recentInquiries,
        List<CropAdvisoryNoticeDto> cropAdvisories,
        List<ExpertScheduleSlotDto> upcomingSchedule
) {}
