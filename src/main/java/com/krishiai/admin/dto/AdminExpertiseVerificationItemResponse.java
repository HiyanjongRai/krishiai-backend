package com.krishiai.admin.dto;

import com.krishiai.expert.entity.ExpertCropExpertise;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.user.entity.User;

import java.time.LocalDateTime;

public record AdminExpertiseVerificationItemResponse(
        Long expertiseId,
        Long expertProfileId,
        Long expertUserId,
        String expertName,
        String expertEmail,
        String expertPhone,
        String professionalType,
        String qualification,
        Integer expertExperienceYears,
        boolean professionalVerified,
        String professionalStatus,
        Long cropId,
        String cropName,
        String cropEmoji,
        String expertiseArea,
        String expertiseType,
        String expertiseLevel,
        Integer claimYearsOfExperience,
        String description,
        String sourceType,
        Long evidenceDocumentId,
        String evidenceTitle,
        String evidenceFileName,
        String evidenceFileUrl,
        String verificationStatus,
        String verificationMethod,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime verifiedAt
) {
    public static AdminExpertiseVerificationItemResponse from(ExpertCropExpertise ece) {
        ExpertProfile ep = ece.getExpertProfile();
        User u = ep != null ? ep.getUser() : null;

        String expertName = u != null && u.getFullName() != null ? u.getFullName() : "Unknown Specialist";
        String expertEmail = u != null ? u.getEmail() : "";
        String expertPhone = u != null ? u.getPhone() : "";

        Long cropId = ece.getCrop() != null ? ece.getCrop().getId() : null;
        String cropName = ece.getCrop() != null ? ece.getCrop().getName() : ece.getExpertiseArea();
        String cropEmoji = ece.getCrop() != null ? ece.getCrop().getEmoji() : "🌿";

        Long docId = ece.getEvidenceDocument() != null ? ece.getEvidenceDocument().getId() : null;
        String docTitle = ece.getEvidenceDocument() != null ? ece.getEvidenceDocument().getTitle() : null;
        String docFileName = ece.getEvidenceDocument() != null ? ece.getEvidenceDocument().getFileName() : null;
        String docFileUrl = ece.getEvidenceDocument() != null ? ece.getEvidenceDocument().getFileUrl() : null;

        return new AdminExpertiseVerificationItemResponse(
                ece.getId(),
                ep != null ? ep.getId() : null,
                u != null ? u.getId() : null,
                expertName,
                expertEmail,
                expertPhone,
                ep != null ? ep.getDesignation() : null,
                ep != null ? ep.getQualification() : null,
                ep != null ? ep.getYearsOfExperience() : null,
                ep != null && ep.isVerifiedExpert(),
                ep != null && ep.getVerificationStatus() != null ? ep.getVerificationStatus().name() : "UNVERIFIED",
                cropId,
                cropName,
                cropEmoji,
                ece.getExpertiseArea(),
                ece.getExpertiseType() != null ? ece.getExpertiseType().name() : "SECONDARY",
                ece.getExpertiseLevel() != null ? ece.getExpertiseLevel().name() : "INTERMEDIATE",
                ece.getYearsOfExperience(),
                ece.getDescription(),
                ece.getSourceType() != null ? ece.getSourceType().name() : "SELF_DECLARED",
                docId,
                docTitle,
                docFileName,
                docFileUrl,
                ece.getVerificationStatus() != null ? ece.getVerificationStatus().name() : "SELF_DECLARED",
                ece.getVerificationMethod() != null ? ece.getVerificationMethod().name() : "NONE",
                ece.getRejectionReason(),
                ece.getUpdatedAt() != null ? ece.getUpdatedAt() : ece.getCreatedAt(),
                ece.getVerifiedAt()
        );
    }
}
