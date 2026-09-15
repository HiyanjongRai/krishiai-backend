package com.krishiai.crop.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.crop.dto.CropCategoryResponse;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.service.CropService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crop-categories")
@Validated
@RequiredArgsConstructor
public class CropCategoryController {

    private final CropService cropService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CropCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Crop categories retrieved successfully",
                cropService.getAllActiveCategories()));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CropCategoryResponse>> getCategoryById(@PathVariable @Positive Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Crop category retrieved successfully",
                cropService.getActiveCategoryById(categoryId)));
    }

    @GetMapping("/{categoryId}/crops")
    public ResponseEntity<ApiResponse<List<CropResponse>>> getCropsByCategory(@PathVariable @Positive Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Category crops retrieved successfully",
                cropService.getActiveCropsByCategory(categoryId)));
    }
}
