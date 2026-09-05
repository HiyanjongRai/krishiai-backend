package com.krishiai.expert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveExpertDocumentRequest(
        @NotBlank(message = "Document type is required")
        @Size(max = 50, message = "Document type must not exceed 50 characters")
        String documentType,

        @NotBlank(message = "Document title is required")
        @Size(max = 200, message = "Document title must not exceed 200 characters")
        String title,

        @NotBlank(message = "File name is required")
        @Size(max = 255, message = "File name must not exceed 255 characters")
        String fileName,

        @Size(max = 100, message = "File type must not exceed 100 characters")
        String fileType,

        @Size(max = 50, message = "File size must not exceed 50 characters")
        String fileSize,

        String fileUrl
) {}
