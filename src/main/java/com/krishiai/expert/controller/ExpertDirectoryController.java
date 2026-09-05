package com.krishiai.expert.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.expert.dto.VerifiedExpertResponse;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.repository.ExpertProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public directory for farmers to search and match with verified agricultural experts.
 *
 * <p>Enforces Requirement 14:
 * Expert search/matching ONLY returns experts where:
 * User.status = ACTIVE
 * AND ExpertProfile.verificationStatus = VERIFIED
 * AND ExpertCropExpertise.verificationStatus = VERIFIED
 *
 * Unverified experts or unverified individual crops are completely excluded.
 */
@RestController
@RequestMapping("/api/v1/experts")
@RequiredArgsConstructor
public class ExpertDirectoryController {

    private final ExpertProfileRepository expertProfileRepository;

    /**
     * GET /api/v1/experts
     * Lists all verified experts, optionally filtered by crop.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VerifiedExpertResponse>>> getVerifiedExperts(
            @RequestParam(required = false) String crop) {

        List<ExpertProfile> profiles;
        if (crop != null && !crop.isBlank()) {
            profiles = expertProfileRepository.findVerifiedExpertsByCropName(crop.trim());
        } else {
            profiles = expertProfileRepository.findAllVerifiedExperts();
        }

        List<VerifiedExpertResponse> responses = profiles.stream()
                .map(VerifiedExpertResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Verified experts retrieved successfully", responses));
    }

    /**
     * GET /api/v1/experts/search
     * Search endpoint specifically for crop-based matching.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VerifiedExpertResponse>>> searchByCrop(
            @RequestParam String crop) {

        List<ExpertProfile> profiles = expertProfileRepository.findVerifiedExpertsByCropName(crop.trim());
        List<VerifiedExpertResponse> responses = profiles.stream()
                .map(VerifiedExpertResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Crop-matched verified experts retrieved", responses));
    }
}
