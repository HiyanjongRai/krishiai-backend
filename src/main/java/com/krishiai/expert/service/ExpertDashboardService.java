package com.krishiai.expert.service;

import com.krishiai.expert.dto.ExpertDashboardResponse;
import com.krishiai.expert.dto.FarmerInquiryDto;
import com.krishiai.expert.dto.UpdateInquiryRequest;

import java.util.List;

public interface ExpertDashboardService {
    ExpertDashboardResponse getDashboard(Long userId);
    List<FarmerInquiryDto> getInquiries(Long userId);
    FarmerInquiryDto updateInquiry(Long userId, Long inquiryId, UpdateInquiryRequest request);
}
