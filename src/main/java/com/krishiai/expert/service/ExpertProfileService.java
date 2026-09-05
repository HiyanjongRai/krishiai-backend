package com.krishiai.expert.service;

import com.krishiai.expert.dto.*;
import com.krishiai.expert.entity.ExpertProfile;

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
     * Adds a crop expertise to the expert's profile.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>At most 3 PRIMARY crops per expert.</li>
     *   <li>A crop cannot be added twice; if already present, its type is updated.</li>
     * </ul>
     *
     * @param userId  the authenticated expert's user ID
     * @param request crop ID and expertise type
     * @return the created or updated crop expertise entry
     */
    CropExpertiseResponse addOrUpdateCropExpertise(Long userId, AddCropExpertiseRequest request);

    /**
     * Removes a crop expertise from the expert's profile.
     *
     * @param userId the authenticated expert's user ID
     * @param cropId the crop to remove
     */
    void removeCropExpertise(Long userId, Long cropId);

    /**
     * Adds a specialization to the expert's profile.
     *
     * @param userId           the authenticated expert's user ID
     * @param specializationId the specialization to add
     * @return the created specialization link
     */
    SpecializationResponse addSpecialization(Long userId, Long specializationId);

    /**
     * Removes a specialization from the expert's profile.
     *
     * @param userId           the authenticated expert's user ID
     * @param specializationId the specialization to remove
     */
    void removeSpecialization(Long userId, Long specializationId);

    /**
     * Adds a location to the expert's service area.
     *
     * @param userId     the authenticated expert's user ID
     * @param locationId the location to add
     * @return the created location link
     */
    LocationResponse addLocation(Long userId, Long locationId);

    /**
     * Removes a location from the expert's service area.
     *
     * @param userId     the authenticated expert's user ID
     * @param locationId the location to remove
     */
    void removeLocation(Long userId, Long locationId);

    /**
     * Submits the expert's verification application for admin review.
     *
     * @param userId the authenticated expert's user ID
     * @return updated profile with applicationStatus = SUBMITTED
     */
    ExpertProfileResponse submitApplication(Long userId);

    /**
     * Internal helper: ensures an ExpertProfile row exists for the given User.
     * Used at registration time to create the profile immediately.
     *
     * @param userId the new expert's user ID
     * @return the (possibly newly created) ExpertProfile
     */
    ExpertProfile ensureProfileExists(Long userId);

    /**
     * Saves or updates a verification document for the expert.
     */
    ExpertDocumentResponse saveDocument(Long userId, SaveExpertDocumentRequest request);

    /**
     * Retrieves all documents uploaded by this expert.
     */
    java.util.List<ExpertDocumentResponse> getDocuments(Long userId);
}
