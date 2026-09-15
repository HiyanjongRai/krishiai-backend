package com.krishiai.crop.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.common.response.PageResponse;
import com.krishiai.crop.dto.CreateCropRequest;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.dto.UpdateCropRequest;
import com.krishiai.crop.service.AdminCropService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/crops")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Validated
@RequiredArgsConstructor
public class AdminCropController {

    private final AdminCropService adminCropService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CropResponse>>> getCrops(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Crops retrieved successfully",
                adminCropService.getCrops(categoryId, search, activeOnly, pageable)));
    }

    @GetMapping("/{cropId}")
    public ResponseEntity<ApiResponse<CropResponse>> getCropById(@PathVariable @Positive Long cropId) {
        return ResponseEntity.ok(ApiResponse.success("Crop retrieved successfully", adminCropService.getCropById(cropId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CropResponse>> createCrop(@Valid @RequestBody CreateCropRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Crop created successfully", adminCropService.createCrop(request)));
    }

    @PutMapping("/{cropId}")
    public ResponseEntity<ApiResponse<CropResponse>> updateCrop(
            @PathVariable @Positive Long cropId,
            @Valid @RequestBody UpdateCropRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Crop updated successfully", adminCropService.updateCrop(cropId, request)));
    }

    @PatchMapping("/{cropId}/status")
    public ResponseEntity<ApiResponse<CropResponse>> updateStatus(
            @PathVariable @Positive Long cropId,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Crop status updated successfully", adminCropService.updateStatus(cropId, active)));
    }

    @DeleteMapping("/{cropId}")
    public ResponseEntity<ApiResponse<Void>> deleteCrop(@PathVariable @Positive Long cropId) {
        adminCropService.deleteCrop(cropId);
        return ResponseEntity.ok(ApiResponse.success("Crop deactivated successfully"));
    }
}
