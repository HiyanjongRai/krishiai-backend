package com.krishiai.expert.service;

import com.krishiai.expert.dto.*;
import com.krishiai.expert.entity.ExpertProfile;

import java.util.List;

public interface ExpertProfileService {

    /**
     * Retrieves or lazily creates the expert profile for the authenticated user.
     *
     * @param userId the authenticated expert's user ID
     * @return full profile response with crops, specializations, and locations
     */
    ExpertProfileResponse getMyProfile(Long userId);

    /**
     * Updates the professional bio and credentials of the expert profile.
     *
     * @param userId  the authenticated expert's user ID
     * @param request patch payload (null fields are ignored)
     * @return updated profile response
     */
    ExpertProfileResponse updateMyProfile(Long userId, UpdateExpertProfileRequest request);

    /**
     * Adds or updates a crop or domain expertise entry.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>At most 3 PRIMARY crops per expert.</li>
     *   <li>Does NOT change the expert's professional verification status.</li>
     *   <li>Expertise starts as SELF_DECLARED, or EVIDENCE_SUBMITTED if evidence document is provided.</li>
     *   <li>Client cannot forge verification status.</li>
     * </ul>
     */
    CropExpertiseResponse addOrUpdateCropExpertise(Long userId, AddCropExpertiseRequest request);

    /**
     * Attaches supporting evidence to an existing expertise claim.
     * Transitions verification status to EVIDENCE_SUBMITTED.
     */
    CropExpertiseResponse attachEvidenceToExpertise(Long userId, Long expertiseId, AttachExpertiseEvidenceRequest request);

    /**
     * Removes an expertise entry by its unique expertise ID.
     * Only unverified claims or claims owned by this expert can be deleted.
     */
    void removeExpertise(Long userId, Long expertiseId);

    /**
     * Removes a crop expertise from the expert's profile by crop ID (backward compatibility).
     */
    void removeCropExpertise(Long userId, Long cropId);

    /**
     * Retrieves all expertise claims for the authenticated expert.
     */
    List<CropExpertiseResponse> getMyExpertises(Long userId);

    /**
     * Adds a specialization to the expert's profile.
     */
    SpecializationResponse addSpecialization(Long userId, Long specializationId);

    /**
     * Removes a specialization from the expert's profile.
     */
    void removeSpecialization(Long userId, Long specializationId);

    /**
     * Adds a location to the expert's service area.
     */
    LocationResponse addLocation(Long userId, Long locationId);

    /**
     * Removes a location from the expert's service area.
     */
    void removeLocation(Long userId, Long locationId);

    /**
     * Submits the expert's professional verification application for admin review.
     */
    ExpertProfileResponse submitApplication(Long userId);

    /**
     * Internal helper: ensures an ExpertProfile row exists for the given User.
     */
    ExpertProfile ensureProfileExists(Long userId);

    /**
     * Saves or updates a verification document for the expert.
     */
    ExpertDocumentResponse saveDocument(Long userId, SaveExpertDocumentRequest request);

    /**
     * Retrieves all documents uploaded by this expert.
     */
    List<ExpertDocumentResponse> getDocuments(Long userId);
}
