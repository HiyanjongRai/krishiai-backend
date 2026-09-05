package com.krishiai.admin.controller;

import com.krishiai.admin.dto.AdminDashboardStatsResponse;
import com.krishiai.admin.dto.ExpertSummaryResponse;
import com.krishiai.admin.dto.FarmerSummaryResponse;
import com.krishiai.admin.dto.PendingExpertApplicationResponse;
import com.krishiai.admin.dto.ReviewApplicationRequest;
import com.krishiai.admin.service.AdminDashboardService;
import com.krishiai.common.response.ApiResponse;
import com.krishiai.expert.dto.CropExpertiseResponse;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        AdminDashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard stats retrieved successfully", stats));
    }

    @GetMapping("/farmers")
    public ResponseEntity<ApiResponse<List<FarmerSummaryResponse>>> getFarmers() {
        List<FarmerSummaryResponse> farmers = adminDashboardService.getFarmers();
        return ResponseEntity.ok(ApiResponse.success("Farmers retrieved successfully", farmers));
    }

    @GetMapping("/experts/all")
    public ResponseEntity<ApiResponse<List<ExpertSummaryResponse>>> getAllExperts() {
        List<ExpertSummaryResponse> experts = adminDashboardService.getAllExperts();
        return ResponseEntity.ok(ApiResponse.success("All experts retrieved successfully", experts));
    }

    @GetMapping("/experts/{profileId}")
    public ResponseEntity<ApiResponse<PendingExpertApplicationResponse>> getExpertDetails(@PathVariable Long profileId) {
        PendingExpertApplicationResponse response = adminDashboardService.getExpertDetails(profileId);
        return ResponseEntity.ok(ApiResponse.success("Expert details retrieved successfully", response));
    }

    @GetMapping("/experts/pending")
    public ResponseEntity<ApiResponse<List<PendingExpertApplicationResponse>>> getPendingExpertApplications() {
        List<PendingExpertApplicationResponse> applications = adminDashboardService.getPendingExpertApplications();
        return ResponseEntity.ok(ApiResponse.success("Pending expert applications retrieved successfully", applications));
    }

    @PostMapping("/experts/{profileId}/start-review")
    public ResponseEntity<ApiResponse<PendingExpertApplicationResponse>> startReview(
            @PathVariable Long profileId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PendingExpertApplicationResponse response = adminDashboardService.startReview(
                profileId, principal.getUserId(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Application is now under review", response));
    }

    @PostMapping("/experts/{profileId}/request-info")
    public ResponseEntity<ApiResponse<PendingExpertApplicationResponse>> requestAdditionalInfo(
            @PathVariable Long profileId,
            @Valid @RequestBody(required = false) ReviewApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PendingExpertApplicationResponse response = adminDashboardService.requestAdditionalInfo(
                profileId, request, principal.getUserId(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Additional information requested from expert", response));
    }

    @PostMapping("/experts/{profileId}/approve")
    public ResponseEntity<ApiResponse<PendingExpertApplicationResponse>> approveExpertApplication(
            @PathVariable Long profileId,
            @Valid @RequestBody(required = false) ReviewApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PendingExpertApplicationResponse response = adminDashboardService.approveExpertApplication(
                profileId, request, principal.getUserId(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Expert application approved successfully", response));
    }

    @PostMapping("/experts/{profileId}/reject")
    public ResponseEntity<ApiResponse<PendingExpertApplicationResponse>> rejectExpertApplication(
            @PathVariable Long profileId,
            @Valid @RequestBody(required = false) ReviewApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PendingExpertApplicationResponse response = adminDashboardService.rejectExpertApplication(
                profileId, request, principal.getUserId(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Expert application rejected", response));
    }

    @PostMapping("/experts/{profileId}/crops/{cropId}/verify")
    public ResponseEntity<ApiResponse<CropExpertiseResponse>> verifyCropExpertise(
            @PathVariable Long profileId,
            @PathVariable Long cropId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        CropExpertiseResponse response = adminDashboardService.verifyCropExpertise(
                profileId, cropId, principal.getUserId(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Crop expertise verified successfully", response));
    }

    @PostMapping("/experts/{profileId}/crops/{cropId}/reject")
    public ResponseEntity<ApiResponse<CropExpertiseResponse>> rejectCropExpertise(
            @PathVariable Long profileId,
            @PathVariable Long cropId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        CropExpertiseResponse response = adminDashboardService.rejectCropExpertise(
                profileId, cropId, principal.getUserId(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Crop expertise rejected", response));
    }
}
