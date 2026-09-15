package com.krishiai.crop.dto;

import com.krishiai.crop.entity.CropCategory;

public record CropCategoryResponse(
        Long id,
        String name,
        String code,
        String description,
        String icon,
        boolean defaultCategory,
        long cropCount,
        boolean active
) {
    public static CropCategoryResponse from(CropCategory category) {
        return new CropCategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                category.getIcon(),
                category.isDefaultCategory(),
                category.getCrops() != null ? category.getCrops().size() : 0,
                category.isActive()
        );
    }
}
