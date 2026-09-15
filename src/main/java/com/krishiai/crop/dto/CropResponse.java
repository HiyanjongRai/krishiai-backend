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
        String imageUrl,
        String description,
        boolean defaultCrop,
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
                crop.getImageUrl(),
                crop.getDescription(),
                crop.isDefaultCrop(),
                crop.isActive()
        );
    }
}
