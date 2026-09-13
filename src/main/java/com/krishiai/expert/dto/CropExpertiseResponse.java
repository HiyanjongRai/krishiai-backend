package com.krishiai.expert.dto;

import com.krishiai.expert.entity.*;

import java.time.LocalDateTime;

public record CropExpertiseResponse(
        Long id,
        Long cropId,
        String cropName,
        String cropEmoji,
        String categoryName,
        CropExpertiseType expertiseType,
        CropExpertiseVerificationStatus verificationStatus,
        LocalDateTime verifiedAt,
        String expertiseArea,
        ExpertiseLevel expertiseLevel,
        Integer yearsOfExperience,
        String description,
        ExpertiseSourceType sourceType,
        Long evidenceDocumentId,
        String evidenceDocumentTitle,
        String evidenceDocumentUrl,
        ExpertiseVerificationMethod verificationMethod,
        Long verifiedBy,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CropExpertiseResponse from(ExpertCropExpertise ece) {
        Long cropId = ece.getCrop() != null ? ece.getCrop().getId() : null;
        String cropName = ece.getCrop() != null ? ece.getCrop().getName() : ece.getExpertiseArea();
        String cropEmoji = ece.getCrop() != null ? ece.getCrop().getEmoji() : "🌿";
        String categoryName = ece.getCrop() != null && ece.getCrop().getCategory() != null
                ? ece.getCrop().getCategory().getName()
                : (ece.getExpertiseArea() != null ? ece.getExpertiseArea() : "General");

        Long evidenceDocId = ece.getEvidenceDocument() != null ? ece.getEvidenceDocument().getId() : null;
        String evidenceDocTitle = ece.getEvidenceDocument() != null ? ece.getEvidenceDocument().getTitle() : null;
        String evidenceDocUrl = ece.getEvidenceDocument() != null ? ece.getEvidenceDocument().getFileUrl() : null;

        return new CropExpertiseResponse(
                ece.getId(),
                cropId,
                cropName,
                cropEmoji,
                categoryName,
                ece.getExpertiseType(),
                ece.getVerificationStatus() != null ? ece.getVerificationStatus() : CropExpertiseVerificationStatus.SELF_DECLARED,
                ece.getVerifiedAt(),
                ece.getExpertiseArea(),
                ece.getExpertiseLevel() != null ? ece.getExpertiseLevel() : ExpertiseLevel.INTERMEDIATE,
                ece.getYearsOfExperience(),
                ece.getDescription(),
                ece.getSourceType() != null ? ece.getSourceType() : ExpertiseSourceType.SELF_DECLARED,
                evidenceDocId,
                evidenceDocTitle,
                evidenceDocUrl,
                ece.getVerificationMethod() != null ? ece.getVerificationMethod() : ExpertiseVerificationMethod.NONE,
                ece.getVerifiedBy(),
                ece.getRejectionReason(),
                ece.getCreatedAt(),
                ece.getUpdatedAt()
        );
    }
}
