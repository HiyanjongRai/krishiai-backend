package com.krishiai.expert.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request body to update professional profile fields.
 * All fields are optional — only non-null values will be updated.
 */
public record UpdateExpertProfileRequest(
        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 80, message = "Years of experience must be realistic")
        Integer yearsOfExperience,

        @Size(max = 200, message = "Qualification must not exceed 200 characters")
        String qualification,

        @Size(max = 200, message = "Institution must not exceed 200 characters")
        String institution,

        @Size(max = 200, message = "Organization must not exceed 200 characters")
        String organization,

        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designation,

        @Size(max = 500, message = "Website URL must not exceed 500 characters")
        String websiteUrl
) {}
