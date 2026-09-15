package com.krishiai.crop.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.crop.dto.CreateCropCategoryRequest;
import com.krishiai.crop.dto.CropCategoryResponse;
import com.krishiai.crop.dto.UpdateCropCategoryRequest;
import com.krishiai.crop.service.AdminCropCategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/crop-categories")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Validated
@RequiredArgsConstructor
public class AdminCropCategoryController {

    private final AdminCropCategoryService adminCropCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropCategoryResponse>>> getAllCategories(
            @RequestParam(required = false) Boolean activeOnly) {
        List<CropCategoryResponse> categories = adminCropCategoryService.getAllCategories(activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Crop categories retrieved successfully", categories));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CropCategoryResponse>> getCategoryById(
            @PathVariable @Positive Long categoryId) {
        CropCategoryResponse category = adminCropCategoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Crop category retrieved successfully", category));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CropCategoryResponse>> createCategory(
            @Valid @RequestBody CreateCropCategoryRequest request) {
        CropCategoryResponse category = adminCropCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Crop category created successfully", category));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CropCategoryResponse>> updateCategory(
            @PathVariable @Positive Long categoryId,
            @Valid @RequestBody UpdateCropCategoryRequest request) {
        CropCategoryResponse category = adminCropCategoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Crop category updated successfully", category));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable @Positive Long categoryId) {
        adminCropCategoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Crop category deleted successfully"));
    }

    @PatchMapping("/{categoryId}/status")
    public ResponseEntity<ApiResponse<CropCategoryResponse>> updateStatus(
            @PathVariable @Positive Long categoryId,
            @RequestParam boolean active) {
        CropCategoryResponse category = adminCropCategoryService.updateStatus(categoryId, active);
        return ResponseEntity.ok(ApiResponse.success("Crop category status updated successfully", category));
    }
}
