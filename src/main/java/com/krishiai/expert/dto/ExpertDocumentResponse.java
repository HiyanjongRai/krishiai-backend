package com.krishiai.expert.dto;

import com.krishiai.expert.entity.ExpertDocument;

import java.time.LocalDateTime;

public record ExpertDocumentResponse(
        Long id,
        String documentType,
        String title,
        String fileName,
        String fileType,
        String fileSize,
        String fileUrl,
        LocalDateTime uploadedAt
) {
    public static ExpertDocumentResponse from(ExpertDocument doc) {
        return new ExpertDocumentResponse(
                doc.getId(),
                doc.getDocumentType(),
                doc.getTitle(),
                doc.getFileName(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getFileUrl(),
                doc.getUploadedAt()
        );
    }
}
