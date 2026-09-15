package com.krishiai.crop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateCropRequest(
        @NotNull(message = "Category is required")
        Long categoryId,

        @NotBlank(message = "Crop name is required")
        @Size(max = 120, message = "Crop name must not exceed 120 characters")
        String name,

        @Size(max = 150, message = "Scientific name must not exceed 150 characters")
        String scientificName,

        @Size(max = 120, message = "Nepali name must not exceed 120 characters")
        String nepaliName,

        @Size(max = 20, message = "Emoji must not exceed 20 characters")
        String emoji,

        @URL(message = "Image URL must be a valid URL")
        @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
        String imageUrl,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        Boolean active
) {
}
