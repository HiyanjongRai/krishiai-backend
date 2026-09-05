package com.krishiai.crop.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.common.response.PageResponse;
import com.krishiai.crop.dto.CropCategoryResponse;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.service.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CropCategoryResponse>>> getCategories() {
        List<CropCategoryResponse> categories = cropService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Crop categories retrieved successfully", categories));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CropResponse>>> getCrops(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<CropResponse> response = cropService.getActiveCrops(categoryId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Crops retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CropResponse>> getCropById(@PathVariable Long id) {
        CropResponse response = cropService.getCropById(id);
        return ResponseEntity.ok(ApiResponse.success("Crop retrieved successfully", response));
    }
}
