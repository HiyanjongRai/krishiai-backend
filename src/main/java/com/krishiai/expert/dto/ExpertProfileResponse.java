package com.krishiai.expert.dto;

import com.krishiai.expert.entity.ExpertApplicationStatus;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.entity.ExpertVerificationStatus;
import com.krishiai.user.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.List;

public record ExpertProfileResponse(
        Long id,
        UserResponse user,
        String bio,
        Integer yearsOfExperience,
        String qualification,
        String institution,
        String organization,
        String designation,
        String websiteUrl,
        boolean verifiedExpert,
        ExpertVerificationStatus verificationStatus,
        ExpertApplicationStatus applicationStatus,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String adminNotes,
        List<CropExpertiseResponse> crops,
        List<SpecializationResponse> specializations,
        List<LocationResponse> locations,
        List<ExpertDocumentResponse> documents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExpertProfileResponse from(ExpertProfile profile) {
        List<ExpertDocumentResponse> docs = profile.getDocuments() == null ? List.of() :
                profile.getDocuments().stream().map(ExpertDocumentResponse::from).toList();

        return new ExpertProfileResponse(
                profile.getId(),
                UserResponse.from(profile.getUser()),
                profile.getBio(),
                profile.getYearsOfExperience(),
                profile.getQualification(),
                profile.getInstitution(),
                profile.getOrganization(),
                profile.getDesignation(),
                profile.getWebsiteUrl(),
                profile.isVerifiedExpert(),
                profile.getVerificationStatus() != null ? profile.getVerificationStatus() : ExpertVerificationStatus.UNVERIFIED,
                profile.getApplicationStatus(),
                profile.getSubmittedAt(),
                profile.getReviewedAt(),
                profile.getAdminNotes(),
                profile.getCropExpertises().stream()
                        .map(CropExpertiseResponse::from)
                        .toList(),
                profile.getSpecializations().stream()
                        .map(SpecializationResponse::from)
                        .toList(),
                profile.getLocations().stream()
                        .map(LocationResponse::from)
                        .toList(),
                docs,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
