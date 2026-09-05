package com.krishiai.crop.dto;

import com.krishiai.crop.entity.Crop;

public record CropResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String scientificName,
        String nepaliName,
        String emoji,
        String description,
        boolean active
) {
    public static CropResponse from(Crop crop) {
        return new CropResponse(
                crop.getId(),
                crop.getCategory() != null ? crop.getCategory().getId() : null,
                crop.getCategory() != null ? crop.getCategory().getName() : null,
                crop.getName(),
                crop.getScientificName(),
                crop.getNepaliName(),
                crop.getEmoji(),
                crop.getDescription(),
                crop.isActive()
        );
    }
}
