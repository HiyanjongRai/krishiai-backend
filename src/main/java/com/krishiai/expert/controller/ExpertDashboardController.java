package com.krishiai.expert.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.expert.dto.ExpertDashboardResponse;
import com.krishiai.expert.dto.FarmerInquiryDto;
import com.krishiai.expert.dto.UpdateInquiryRequest;
import com.krishiai.expert.service.ExpertDashboardService;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Expert Dashboard.
 *
 * <p>Secured for experts and admins (for previewing).
 * Base path: {@code /api/v1/expert/dashboard}
 */
@RestController
@RequestMapping("/api/v1/expert/dashboard")
@PreAuthorize("hasAuthority('ROLE_EXPERT')")
@Validated
@RequiredArgsConstructor
public class ExpertDashboardController {

    private final ExpertDashboardService expertDashboardService;

    /**
     * GET /api/v1/expert/dashboard
     * Returns full dynamic dashboard payload for the authenticated expert.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ExpertDashboardResponse>> getDashboard(
            @AuthenticationPrincipal CustomUserDetails principal) {

        ExpertDashboardResponse response = expertDashboardService.getDashboard(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Expert dashboard loaded successfully", response));
    }

    /**
     * GET /api/v1/expert/dashboard/inquiries
     * Returns list of farmer inquiries assigned to this expert.
     */
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<List<FarmerInquiryDto>>> getInquiries(
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<FarmerInquiryDto> inquiries = expertDashboardService.getInquiries(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Farmer inquiries retrieved successfully", inquiries));
    }

    /**
     * POST /api/v1/expert/dashboard/inquiries/{id}/reply
     * Submits expert review/status update for an inquiry.
     * STRICT VERIFICATION BOUNDARY: Only ACTIVE and verified experts (or ADMINs) can provide verified advice.
     */
    @PostMapping("/inquiries/{id}/reply")
    @PreAuthorize("@expertAuth.isVerifiedExpert(principal.userId)")
    public ResponseEntity<ApiResponse<FarmerInquiryDto>> replyInquiry(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateInquiryRequest request) {

        FarmerInquiryDto updated = expertDashboardService.updateInquiry(principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Inquiry status updated successfully", updated));
    }
}
