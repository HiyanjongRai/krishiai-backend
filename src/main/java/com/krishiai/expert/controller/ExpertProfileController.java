package com.krishiai.expert.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.expert.dto.*;
import com.krishiai.expert.service.ExpertProfileService;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for expert profile management.
 *
 * <p>All endpoints require {@code ROLE_EXPERT} authority.
 * Secured at the class level — individual method-level security is not needed.
 *
 * <p>Base path: {@code /api/v1/expert/profile}
 */
@RestController
@RequestMapping("/api/v1/expert/profile")
@PreAuthorize("hasAnyAuthority('ROLE_EXPERT', 'ROLE_ADMIN')")
@RequiredArgsConstructor
public class ExpertProfileController {

    private final ExpertProfileService expertProfileService;

    // ─── Profile ──────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/expert/profile
     * Returns the authenticated expert's full profile.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal) {

        ExpertProfileResponse response = expertProfileService.getMyProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    /**
     * PATCH /api/v1/expert/profile
     * Partially updates professional bio and credentials.
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateExpertProfileRequest request) {

        ExpertProfileResponse response = expertProfileService.updateMyProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    // ─── Crop & Domain Expertise ───────────────────────────────────────────────

    /**
     * GET /api/v1/expert/profile/crops
     * List all crop and domain expertise claims for the authenticated expert.
     */
    @GetMapping("/crops")
    public ResponseEntity<ApiResponse<java.util.List<CropExpertiseResponse>>> getMyExpertises(
            @AuthenticationPrincipal CustomUserDetails principal) {

        java.util.List<CropExpertiseResponse> response = expertProfileService.getMyExpertises(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Expertise claims retrieved successfully", response));
    }

    /**
     * POST /api/v1/expert/profile/crops
     * Add or update a crop or domain expertise claim.
     */
    @PostMapping("/crops")
    public ResponseEntity<ApiResponse<CropExpertiseResponse>> addCrop(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddCropExpertiseRequest request) {

        CropExpertiseResponse response = expertProfileService
                .addOrUpdateCropExpertise(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Crop expertise saved successfully", response));
    }

    /**
     * POST /api/v1/expert/profile/expertises/{expertiseId}/evidence
     * Attach verification evidence to an existing expertise claim.
     */
    @PostMapping("/expertises/{expertiseId}/evidence")
    public ResponseEntity<ApiResponse<CropExpertiseResponse>> attachEvidence(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long expertiseId,
            @Valid @RequestBody AttachExpertiseEvidenceRequest request) {

        CropExpertiseResponse response = expertProfileService
                .attachEvidenceToExpertise(principal.getUserId(), expertiseId, request);
        return ResponseEntity.ok(ApiResponse.success("Supporting evidence submitted successfully", response));
    }

    /**
     * DELETE /api/v1/expert/profile/expertises/{expertiseId}
     * Remove an expertise claim by expertise ID.
     */
    @DeleteMapping("/expertises/{expertiseId}")
    public ResponseEntity<ApiResponse<Void>> removeExpertise(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long expertiseId) {

        expertProfileService.removeExpertise(principal.getUserId(), expertiseId);
        return ResponseEntity.ok(ApiResponse.success("Expertise claim removed", null));
    }

    /**
     * DELETE /api/v1/expert/profile/crops/{cropId}
     * Remove a crop from the expert's profile by crop ID (backward compatibility).
     */
    @DeleteMapping("/crops/{cropId}")
    public ResponseEntity<ApiResponse<Void>> removeCrop(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long cropId) {

        expertProfileService.removeCropExpertise(principal.getUserId(), cropId);
        return ResponseEntity.ok(ApiResponse.success("Crop expertise removed", null));
    }

    // ─── Specializations ──────────────────────────────────────────────────────

    /**
     * POST /api/v1/expert/profile/specializations/{specializationId}
     * Add a specialization to the expert's profile.
     */
    @PostMapping("/specializations/{specializationId}")
    public ResponseEntity<ApiResponse<SpecializationResponse>> addSpecialization(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long specializationId) {

        SpecializationResponse response = expertProfileService
                .addSpecialization(principal.getUserId(), specializationId);
        return ResponseEntity.ok(ApiResponse.success("Specialization added successfully", response));
    }

    /**
     * DELETE /api/v1/expert/profile/specializations/{specializationId}
     * Remove a specialization from the expert's profile.
     */
    @DeleteMapping("/specializations/{specializationId}")
    public ResponseEntity<ApiResponse<Void>> removeSpecialization(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long specializationId) {

        expertProfileService.removeSpecialization(principal.getUserId(), specializationId);
        return ResponseEntity.ok(ApiResponse.success("Specialization removed", null));
    }

    // ─── Locations ────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/expert/profile/locations/{locationId}
     * Add a service location to the expert's profile.
     */
    @PostMapping("/locations/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponse>> addLocation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long locationId) {

        LocationResponse response = expertProfileService
                .addLocation(principal.getUserId(), locationId);
        return ResponseEntity.ok(ApiResponse.success("Location added successfully", response));
    }

    /**
     * DELETE /api/v1/expert/profile/locations/{locationId}
     * Remove a service location from the expert's profile.
     */
    @DeleteMapping("/locations/{locationId}")
    public ResponseEntity<ApiResponse<Void>> removeLocation(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long locationId) {

        expertProfileService.removeLocation(principal.getUserId(), locationId);
        return ResponseEntity.ok(ApiResponse.success("Location removed", null));
    }

    // ─── Verification Application ─────────────────────────────────────────────

    /**
     * POST /api/v1/expert/profile/submit-application
     * Submit the verification application for admin review.
     * Only valid when applicationStatus is DRAFT or REJECTED.
     */
    @PostMapping("/submit-application")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> submitApplication(
            @AuthenticationPrincipal CustomUserDetails principal) {

        ExpertProfileResponse response = expertProfileService.submitApplication(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(
                "Verification application submitted. An admin will review it shortly.", response));
    }

    // ─── Documents ────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/expert/profile/documents
     * Upload or update a verification document.
     */
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<ExpertDocumentResponse>> saveDocument(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SaveExpertDocumentRequest request) {

        ExpertDocumentResponse response = expertProfileService
                .saveDocument(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Document saved successfully", response));
    }

    /**
     * GET /api/v1/expert/profile/documents
     * List all verification documents uploaded by this expert.
     */
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<java.util.List<ExpertDocumentResponse>>> getDocuments(
            @AuthenticationPrincipal CustomUserDetails principal) {

        java.util.List<ExpertDocumentResponse> response = expertProfileService
                .getDocuments(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", response));
    }
}
