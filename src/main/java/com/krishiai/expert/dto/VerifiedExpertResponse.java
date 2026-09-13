package com.krishiai.expert.dto;

import com.krishiai.expert.entity.CropExpertiseVerificationStatus;
import com.krishiai.expert.entity.ExpertProfile;

import java.util.List;

/**
 * Public response for verified expert directory & matching.
 * STRICT PRIVACY & VERIFICATION:
 * - Verified crops and all expertise claims are exposed with their explicit verification status.
 * - Sensitive private document paths and internal audit information are omitted.
 */
public record VerifiedExpertResponse(
        Long expertProfileId,
        Long userId,
        String fullName,
        String profileImage,
        String designation,
        String organization,
        Integer yearsOfExperience,
        String qualification,
        String institution,
        String bio,
        boolean professionalVerified,
        String professionalVerificationStatus,
        List<CropExpertiseResponse> verifiedCrops,
        List<CropExpertiseResponse> allExpertises,
        List<String> specializations,
        List<String> locations
) {
    public static VerifiedExpertResponse from(ExpertProfile ep) {
        String fullName = ep.getUser() != null && ep.getUser().getFullName() != null
                ? ep.getUser().getFullName().trim()
                : "Agricultural Specialist";
        String profileImage = ep.getUser() != null ? ep.getUser().getProfileImage() : null;

        List<CropExpertiseResponse> all = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .map(CropExpertiseResponse::from)
                        .toList();

        List<CropExpertiseResponse> verifiedCrops = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .filter(c -> c.getVerificationStatus() == CropExpertiseVerificationStatus.VERIFIED)
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

        return new VerifiedExpertResponse(
                ep.getId(),
                ep.getUser() != null ? ep.getUser().getId() : null,
                fullName,
                profileImage,
                ep.getDesignation(),
                ep.getOrganization(),
                ep.getYearsOfExperience(),
                ep.getQualification(),
                ep.getInstitution(),
                ep.getBio(),
                ep.isVerifiedExpert(),
                ep.getVerificationStatus() != null ? ep.getVerificationStatus().name() : "UNVERIFIED",
                verifiedCrops,
                all,
                specs,
                locs
        );
    }
}
