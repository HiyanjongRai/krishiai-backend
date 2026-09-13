package com.krishiai.admin.service;

import com.krishiai.admin.dto.*;
import com.krishiai.expert.dto.CropExpertiseResponse;

import java.util.List;

public interface AdminDashboardService {
    AdminDashboardStatsResponse getDashboardStats();
    List<PendingExpertApplicationResponse> getPendingExpertApplications();
    PendingExpertApplicationResponse approveExpertApplication(Long profileId, ReviewApplicationRequest request, Long adminUserId, String adminEmail);
    PendingExpertApplicationResponse rejectExpertApplication(Long profileId, ReviewApplicationRequest request, Long adminUserId, String adminEmail);
    PendingExpertApplicationResponse startReview(Long profileId, Long adminUserId, String adminEmail);
    PendingExpertApplicationResponse requestAdditionalInfo(Long profileId, ReviewApplicationRequest request, Long adminUserId, String adminEmail);
    CropExpertiseResponse verifyCropExpertise(Long profileId, Long cropId, Long adminUserId, String adminEmail);
    CropExpertiseResponse rejectCropExpertise(Long profileId, Long cropId, Long adminUserId, String adminEmail);
    BatchExpertiseVerificationResponse batchVerifyExpertises(BatchExpertiseVerificationRequest request, Long adminUserId, String adminEmail);
    List<AdminExpertiseVerificationItemResponse> getExpertiseVerifications(String status);
    List<FarmerSummaryResponse> getFarmers();
    List<ExpertSummaryResponse> getAllExperts();
    PendingExpertApplicationResponse getExpertDetails(Long profileId);
}
