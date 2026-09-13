package com.krishiai.farm.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.farm.dto.FarmerDashboardResponse;
import com.krishiai.farm.service.FarmerDashboardService;
import com.krishiai.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farmer")
@PreAuthorize("hasAuthority('ROLE_FARMER')")
@RequiredArgsConstructor
public class FarmerDashboardController {

    private final FarmerDashboardService farmerDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<FarmerDashboardResponse>> getDashboard(
            @AuthenticationPrincipal CustomUserDetails principal) {
        FarmerDashboardResponse response = farmerDashboardService.getDashboard(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Farmer dashboard retrieved successfully", response));
    }
}
