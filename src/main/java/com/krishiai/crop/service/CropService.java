package com.krishiai.crop.service;

import com.krishiai.common.response.PageResponse;
import com.krishiai.crop.dto.CropCategoryResponse;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.entity.Crop;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CropService {
    List<CropCategoryResponse> getAllActiveCategories();
    PageResponse<CropResponse> getActiveCrops(Long categoryId, String search, Pageable pageable);
    CropResponse getCropById(Long id);
    Crop getCropEntity(Long id);
}
