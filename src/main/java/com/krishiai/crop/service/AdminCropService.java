package com.krishiai.crop.service;

import com.krishiai.common.response.PageResponse;
import com.krishiai.crop.dto.CreateCropRequest;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.dto.UpdateCropRequest;
import org.springframework.data.domain.Pageable;

public interface AdminCropService {
    PageResponse<CropResponse> getCrops(Long categoryId, String search, Boolean activeOnly, Pageable pageable);
    CropResponse getCropById(Long cropId);
    CropResponse createCrop(CreateCropRequest request);
    CropResponse updateCrop(Long cropId, UpdateCropRequest request);
    CropResponse updateStatus(Long cropId, boolean active);
    void deleteCrop(Long cropId);
}
