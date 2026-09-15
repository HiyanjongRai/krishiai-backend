package com.krishiai.admin.dto;

import jakarta.validation.constraints.Size;

public record ReviewApplicationRequest(
        @Size(min = 3, max = 1000, message = "Admin notes must be between 3 and 1000 characters")
        String notes
) {}
