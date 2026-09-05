package com.krishiai.admin.dto;

import jakarta.validation.constraints.Size;

public record ReviewApplicationRequest(
        @Size(max = 1000, message = "Admin notes must not exceed 1000 characters")
        String notes
) {}
