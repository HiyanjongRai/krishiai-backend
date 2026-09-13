package com.krishiai.expert.dto;

import com.krishiai.expert.entity.ExpertDocument;
import com.krishiai.expert.entity.ExpertDocumentStatus;

import java.time.LocalDateTime;

public record ExpertDocumentResponse(
        Long id,
        Long expertProfileId,
        String documentType,
        String title,
        String fileName,
        String fileType,
        String fileSize,
        String fileUrl,
        LocalDateTime uploadedAt,
        ExpertDocumentStatus status,
        String rejectionReason,
        LocalDateTime verifiedAt
) {
    public ExpertDocumentResponse(Long id, String documentType, String title, String fileName,
                                  String fileType, String fileSize, String fileUrl, LocalDateTime uploadedAt) {
        this(id, null, documentType, title, fileName, fileType, fileSize, fileUrl, uploadedAt, ExpertDocumentStatus.PENDING, null, null);
    }

    public static ExpertDocumentResponse from(ExpertDocument doc) {
        return new ExpertDocumentResponse(
                doc.getId(),
                doc.getExpertProfile() != null ? doc.getExpertProfile().getId() : null,
                doc.getDocumentType(),
                doc.getTitle(),
                doc.getFileName(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getFileUrl(),
                doc.getUploadedAt(),
                doc.getStatus(),
                doc.getRejectionReason(),
                doc.getVerifiedAt()
        );
    }
}
