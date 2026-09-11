package com.krishiai.admin.dto;

import com.krishiai.expert.dto.CropExpertiseResponse;
import com.krishiai.expert.dto.ExpertDocumentResponse;
import com.krishiai.expert.entity.CropExpertiseType;
import com.krishiai.expert.entity.ExpertApplicationStatus;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.entity.ExpertVerificationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PendingExpertApplicationResponse(
        Long profileId,
        Long userId,
        String fullName,
        String email,
        String phone,
        String designation,
        String organization,
        Integer yearsOfExperience,
        String qualification,
        String institution,
        String bio,
        String websiteUrl,
        List<String> primaryCrops,
        List<String> secondaryCrops,
        List<CropExpertiseResponse> cropDetails,
        List<String> specializations,
        List<String> locations,
        List<ExpertDocumentResponse> documents,
        boolean verifiedExpert,
        ExpertVerificationStatus verificationStatus,
        ExpertApplicationStatus applicationStatus,
        String adminNotes,
        LocalDateTime submittedAt
) {
    public static PendingExpertApplicationResponse from(ExpertProfile ep) {
        String fullName = ep.getUser() != null
                ? (ep.getUser().getFullName() + " " + ep.getUser()).trim()
                : "Unknown Expert";
        String email = ep.getUser() != null ? ep.getUser().getEmail() : null;
        String phone = ep.getUser() != null ? ep.getUser().getPhone() : null;

        List<String> primary = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .filter(c -> c.getExpertiseType() == CropExpertiseType.PRIMARY)
                        .map(c -> c.getCrop().getName())
                        .toList();

        List<String> secondary = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .filter(c -> c.getExpertiseType() == CropExpertiseType.SECONDARY)
                        .map(c -> c.getCrop().getName())
                        .toList();

        List<CropExpertiseResponse> cropDetails = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .map(CropExpertiseResponse::from)
                        .toList();

        List<String> specs = ep.getSpecializations() == null ? List.of() :
                ep.getSpecializations().stream()
                        .map(s -> s.getSpecialization().getName())
                        .toList();

        List<String> locs = ep.getLocations() == null ? List.of() :
                ep.getLocations().stream()
                        .map(l -> l.getLocation().getName())
                        .toList();

        List<ExpertDocumentResponse> docs = ep.getDocuments() == null ? List.of() :
                ep.getDocuments().stream()
                        .map(ExpertDocumentResponse::from)
                        .toList();

        return new PendingExpertApplicationResponse(
                ep.getId(),
                ep.getUser() != null ? ep.getUser().getId() : null,
                fullName,
                email,
                phone,
                ep.getDesignation(),
                ep.getOrganization(),
                ep.getYearsOfExperience(),
                ep.getQualification(),
                ep.getInstitution(),
                ep.getBio(),
                ep.getWebsiteUrl(),
                primary,
                secondary,
                cropDetails,
                specs,
                locs,
                docs,
                ep.isVerifiedExpert(),
                ep.getVerificationStatus() != null ? ep.getVerificationStatus() : ExpertVerificationStatus.UNVERIFIED,
                ep.getApplicationStatus(),
                ep.getAdminNotes(),
                ep.getSubmittedAt()
        );
    }
}
