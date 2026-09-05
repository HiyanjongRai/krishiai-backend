package com.krishiai.admin.dto;

import com.krishiai.expert.dto.ExpertDocumentResponse;
import com.krishiai.expert.entity.CropExpertiseType;
import com.krishiai.expert.entity.ExpertApplicationStatus;
import com.krishiai.expert.entity.ExpertProfile;

import java.time.LocalDateTime;
import java.util.List;

public record ExpertSummaryResponse(
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
        boolean verifiedExpert,
        ExpertApplicationStatus applicationStatus,
        String adminNotes,
        List<String> primaryCrops,
        List<String> secondaryCrops,
        List<String> specializations,
        List<String> locations,
        List<ExpertDocumentResponse> documents,
        LocalDateTime createdAt,
        LocalDateTime submittedAt
) {
    public static ExpertSummaryResponse from(ExpertProfile ep) {
        String fullName = ep.getUser() != null
                ? (ep.getUser().getFirstName() + " " + ep.getUser().getLastName()).trim()
                : "Unknown Expert";

        List<String> primaryCrops = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .filter(c -> c.getExpertiseType() == CropExpertiseType.PRIMARY)
                        .map(c -> c.getCrop().getName())
                        .toList();

        List<String> secondaryCrops = ep.getCropExpertises() == null ? List.of() :
                ep.getCropExpertises().stream()
                        .filter(c -> c.getExpertiseType() == CropExpertiseType.SECONDARY)
                        .map(c -> c.getCrop().getName())
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
                        .map(d -> new ExpertDocumentResponse(
                                d.getId(),
                                d.getDocumentType(),
                                d.getTitle(),
                                d.getFileName(),
                                d.getFileType(),
                                d.getFileSize(),
                                null,
                                d.getUploadedAt()
                        ))
                        .toList();

        return new ExpertSummaryResponse(
                ep.getId(),
                ep.getUser() != null ? ep.getUser().getId() : null,
                fullName,
                ep.getUser() != null ? ep.getUser().getEmail() : null,
                ep.getUser() != null ? ep.getUser().getPhone() : null,
                ep.getDesignation(),
                ep.getOrganization(),
                ep.getYearsOfExperience(),
                ep.getQualification(),
                ep.getInstitution(),
                ep.getBio(),
                ep.getWebsiteUrl(),
                ep.isVerifiedExpert(),
                ep.getApplicationStatus(),
                ep.getAdminNotes(),
                primaryCrops,
                secondaryCrops,
                specs,
                locs,
                docs,
                ep.getUser() != null ? ep.getUser().getCreatedAt() : null,
                ep.getSubmittedAt()
        );
    }
}
