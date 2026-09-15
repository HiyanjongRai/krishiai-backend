package com.krishiai.crop.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.common.response.PageResponse;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.service.CropService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crops")
@Validated
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CropResponse>>> getCrops(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Size(max = 100) String search,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<CropResponse> response = cropService.getActiveCrops(categoryId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Crops retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CropResponse>> getCropById(@PathVariable @Positive Long id) {
        CropResponse response = cropService.getCropById(id);
        return ResponseEntity.ok(ApiResponse.success("Crop retrieved successfully", response));
    }
}
