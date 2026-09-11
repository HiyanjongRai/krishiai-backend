package com.krishiai.expert.dto;

import com.krishiai.expert.entity.CropExpertiseVerificationStatus;
import com.krishiai.expert.entity.ExpertProfile;

import java.util.List;

/**
 * Public response for verified expert directory & matching.
 * STRICT PRIVACY & VERIFICATION:
 * - Only verified crops are included.
 * - Sensitive private document paths and internal audit information are omitted.
 */
public record VerifiedExpertResponse(
        Long expertProfileId,
        Long userId,
        String fullName,
        String designation,
        String organization,
        Integer yearsOfExperience,
        String qualification,
        String institution,
        String bio,
        List<CropExpertiseResponse> verifiedCrops,
        List<String> specializations,
        List<String> locations
) {
    public static VerifiedExpertResponse from(ExpertProfile ep) {
        String fullName = ep.getUser() != null
                ? (ep.getUser().getFullName() + " " + ep.getUser()).trim()
                : "Agricultural Specialist";

        // ONLY verified crop expertise is exposed to farmers
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
                ep.getDesignation(),
                ep.getOrganization(),
                ep.getYearsOfExperience(),
                ep.getQualification(),
                ep.getInstitution(),
                ep.getBio(),
                verifiedCrops,
                specs,
                locs
        );
    }
}
