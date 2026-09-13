package com.krishiai.crop.dto;

import jakarta.validation.constraints.Size;

public record UpdateCropCategoryRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Size(max = 100, message = "Icon must not exceed 100 characters")
        String icon,

        Boolean active
) {
}
