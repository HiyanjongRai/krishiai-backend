package com.krishiai.crop.service;

import com.krishiai.crop.dto.CreateCropCategoryRequest;
import com.krishiai.crop.dto.CropCategoryResponse;
import com.krishiai.crop.dto.UpdateCropCategoryRequest;

import java.util.List;

public interface AdminCropCategoryService {

    List<CropCategoryResponse> getAllCategories(Boolean activeOnly);

    CropCategoryResponse getCategoryById(Long categoryId);

    CropCategoryResponse createCategory(CreateCropCategoryRequest request);

    CropCategoryResponse updateCategory(Long categoryId, UpdateCropCategoryRequest request);

    void deleteCategory(Long categoryId);
}
