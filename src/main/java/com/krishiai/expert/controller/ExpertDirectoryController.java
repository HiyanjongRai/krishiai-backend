package com.krishiai.expert.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.expert.dto.VerifiedExpertResponse;
import com.krishiai.expert.entity.CropExpertiseVerificationStatus;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.repository.ExpertProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * Public directory for farmers to search and match with agricultural experts.
 *
 * <p>Enforces Requirement 14 & 15:
 * Distinguishes verified professional status from individual crop/domain expertise.
 * Prioritizes:
 * 1. Verified professional + Verified relevant expertise
 * 2. Verified professional + Evidence-supported expertise
 * 3. Verified professional + Self-declared relevant expertise
 * 4. Experience & ratings
 */
@RestController
@RequestMapping("/api/v1/experts")
@RequiredArgsConstructor
public class ExpertDirectoryController {

    private static final int EXPERT_DIRECTORY_LIMIT = 100;

    private final ExpertProfileRepository expertProfileRepository;

    /**
     * GET /api/v1/experts
     * Lists all experts, prioritizing professionally verified specialists.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VerifiedExpertResponse>>> getVerifiedExperts(
            @RequestParam(required = false) String crop) {

        List<ExpertProfile> profiles;
        if (crop != null && !crop.isBlank()) {
            profiles = searchAndRankExperts(crop.trim());
        } else {
            profiles = expertProfileRepository.findAllVerifiedExperts(PageRequest.of(0, EXPERT_DIRECTORY_LIMIT));
        }

        List<VerifiedExpertResponse> responses = profiles.stream()
                .map(VerifiedExpertResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Experts retrieved successfully", responses));
    }

    /**
     * GET /api/v1/experts/search
     * Search endpoint specifically for crop and domain based matching.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VerifiedExpertResponse>>> searchByCrop(
            @RequestParam String crop) {

        List<ExpertProfile> profiles = searchAndRankExperts(crop.trim());
        List<VerifiedExpertResponse> responses = profiles.stream()
                .map(VerifiedExpertResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Crop-matched experts retrieved", responses));
    }

    private List<ExpertProfile> searchAndRankExperts(String cropQuery) {
        String cleanQuery = cropQuery.toLowerCase();
        List<ExpertProfile> matched = expertProfileRepository.searchExpertsByCropOrDomain(
                cleanQuery,
                PageRequest.of(0, EXPERT_DIRECTORY_LIMIT)
        );

        // Rank by matching score:
        // Score breakdown:
        // +100: Professionally verified
        // +50: Relevant crop/area is VERIFIED
        // +25: Relevant crop/area is EVIDENCE_SUBMITTED
        // +10: Relevant crop/area is SELF_DECLARED
        // +yearsOfExperience
        return matched.stream()
                .sorted(Comparator.comparingInt((ExpertProfile ep) -> {
                    int score = 0;
                    if (ep.isVerifiedExpert()) {
                        score += 100;
                    }

                    if (ep.getCropExpertises() != null) {
                        for (var ece : ep.getCropExpertises()) {
                            boolean matches = (ece.getCrop() != null && ece.getCrop().getName().toLowerCase().contains(cleanQuery))
                                    || (ece.getExpertiseArea() != null && ece.getExpertiseArea().toLowerCase().contains(cleanQuery));
                            if (matches) {
                                if (ece.getVerificationStatus() == CropExpertiseVerificationStatus.VERIFIED) {
                                    score += 50;
                                } else if (ece.getVerificationStatus() == CropExpertiseVerificationStatus.EVIDENCE_SUBMITTED) {
                                    score += 25;
                                } else if (ece.getVerificationStatus() == CropExpertiseVerificationStatus.SELF_DECLARED) {
                                    score += 10;
                                }
                            }
                        }
                    }

                    score += (ep.getYearsOfExperience() != null ? Math.min(ep.getYearsOfExperience(), 20) : 0);
                    return score;
                }).reversed())
                .toList();
    }
}
