package com.krishiai.farm.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.farm.dto.CreateFarmRequest;
import com.krishiai.farm.dto.FarmResponse;
import com.krishiai.farm.dto.UpdateFarmRequest;
import com.krishiai.farm.service.FarmService;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farmer")
@PreAuthorize("hasAuthority('ROLE_FARMER')")
@RequiredArgsConstructor
public class FarmerFarmController {

    private final FarmService farmService;

    // ── Single-Farm Endpoints (/farm) ─────────────────────────────────────────

    @PostMapping("/farm")
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateFarmRequest request) {
        FarmResponse response = farmService.createFarm(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Farm created successfully", response));
    }

    @GetMapping("/farm")
    public ResponseEntity<ApiResponse<FarmResponse>> getMyFarm(
            @AuthenticationPrincipal CustomUserDetails principal) {
        FarmResponse response = farmService.getMyFarm(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Farm retrieved successfully", response));
    }

    @PutMapping("/farm")
    public ResponseEntity<ApiResponse<FarmResponse>> updateMyFarm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateFarmRequest request) {
        FarmResponse response = farmService.updateMyFarm(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Farm updated successfully", response));
    }

    @DeleteMapping("/farm")
    public ResponseEntity<ApiResponse<Void>> deleteMyFarm(
            @AuthenticationPrincipal CustomUserDetails principal) {
        farmService.deleteMyFarm(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Farm deleted successfully"));
    }

    // ── Multi-Farm Endpoints (/farms) ─────────────────────────────────────────

    @GetMapping("/farms")
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getAllMyFarms(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<FarmResponse> response = farmService.getAllMyFarms(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Farms retrieved successfully", response));
    }

    @GetMapping("/farms/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> getMyFarmById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        FarmResponse response = farmService.getMyFarmById(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Farm retrieved successfully", response));
    }

    @PutMapping("/farms/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateMyFarmById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateFarmRequest request) {
        FarmResponse response = farmService.updateMyFarmById(principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Farm updated successfully", response));
    }

    @DeleteMapping("/farms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyFarmById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        farmService.deleteMyFarmById(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Farm deleted successfully"));
    }
}
