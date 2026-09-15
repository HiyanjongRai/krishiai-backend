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
        List<PublicExpertiseResponse> verifiedCrops,
        List<PublicExpertiseResponse> allExpertises,
        List<String> specializations,
        List<String> locations
) {
    public static VerifiedExpertResponse from(ExpertProfile ep) {
        String fullName = ep.getUser() != null && ep.getUser().getFullName() != null
                ? ep.getUser().getFullName().trim()
                : "Agricultural Specialist";
        String profileImage = ep.getUser() != null ? ep.getUser().getProfileImage() : null;

        List<PublicExpertiseResponse> all = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .map(PublicExpertiseResponse::from)
                        .toList();

        List<PublicExpertiseResponse> verifiedCrops = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .filter(c -> c.getVerificationStatus() == CropExpertiseVerificationStatus.VERIFIED)
                        .map(PublicExpertiseResponse::from)
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
