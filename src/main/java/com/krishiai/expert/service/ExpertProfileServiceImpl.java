package com.krishiai.expert.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.repository.CropRepository;
import com.krishiai.expert.dto.*;
import com.krishiai.expert.entity.*;
import com.krishiai.expert.repository.*;
import com.krishiai.location.entity.Location;
import com.krishiai.location.repository.LocationRepository;
import com.krishiai.specialization.entity.Specialization;
import com.krishiai.specialization.repository.SpecializationRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing an expert's professional profile.
 *
 * <p>Key business rules enforced here (not in the DB):
 * <ul>
 *   <li>An expert may have at most 3 PRIMARY crops.</li>
 *   <li>Adding a crop that is already linked updates its type (upsert semantics).</li>
 *   <li>Only experts in DRAFT or REJECTED state may submit an application.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertProfileServiceImpl implements ExpertProfileService {

    /** Maximum number of PRIMARY crops allowed per expert. */
    private static final int MAX_PRIMARY_CROPS = 3;

    private final ExpertProfileRepository profileRepository;
    private final ExpertCropExpertiseRepository cropExpertiseRepository;
    private final ExpertSpecializationRepository specializationRepository;
    private final ExpertLocationRepository locationRepository;
    private final ExpertDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final SpecializationRepository specializationRepo;
    private final LocationRepository locationRepo;

    // ─── Profile ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ExpertProfileResponse getMyProfile(Long userId) {
        ExpertProfile profile = profileRepository.findByUserIdWithDetails(userId)
                .orElseGet(() -> createProfileForUser(userId));
        return ExpertProfileResponse.from(profile);
    }

    @Override
    @Transactional
    public ExpertProfileResponse updateMyProfile(Long userId, UpdateExpertProfileRequest request) {
        ExpertProfile profile = requireProfile(userId);

        // Patch — only update non-null fields
        if (request.bio() != null)                profile.setBio(request.bio());
        if (request.yearsOfExperience() != null)  profile.setYearsOfExperience(request.yearsOfExperience());
        if (request.qualification() != null)       profile.setQualification(request.qualification());
        if (request.institution() != null)         profile.setInstitution(request.institution());
        if (request.organization() != null)        profile.setOrganization(request.organization());
        if (request.designation() != null)         profile.setDesignation(request.designation());
        if (request.websiteUrl() != null)          profile.setWebsiteUrl(request.websiteUrl());

        ExpertProfile saved = profileRepository.save(profile);
        // Re-fetch with details so the response includes nested collections
        return ExpertProfileResponse.from(
                profileRepository.findByUserIdWithDetails(userId).orElse(saved));
    }

    // ─── Crop Expertise ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public CropExpertiseResponse addOrUpdateCropExpertise(Long userId, AddCropExpertiseRequest request) {
        ExpertProfile profile = requireProfile(userId);

        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + request.cropId()));

        // Upsert: if the expert already has this crop, update the type
        ExpertCropExpertise entry = cropExpertiseRepository
                .findByExpertProfileIdAndCropId(profile.getId(), crop.getId())
                .orElse(null);

        if (entry == null) {
            // New entry — check primary cap before inserting
            if (request.expertiseType() == CropExpertiseType.PRIMARY) {
                long currentPrimaryCount = cropExpertiseRepository
                        .countByExpertProfileIdAndExpertiseType(profile.getId(), CropExpertiseType.PRIMARY);
                if (currentPrimaryCount >= MAX_PRIMARY_CROPS) {
                    throw new BadRequestException(
                            "An expert may have at most " + MAX_PRIMARY_CROPS + " primary crops. " +
                            "Please remove one before adding another, or set this crop as SECONDARY."
                    );
                }
            }
            entry = new ExpertCropExpertise(profile, crop, request.expertiseType());
            entry.setVerificationStatus(CropExpertiseVerificationStatus.PENDING);
        } else {
            // Existing entry — check cap only if upgrading to PRIMARY
            if (request.expertiseType() == CropExpertiseType.PRIMARY
                    && entry.getExpertiseType() != CropExpertiseType.PRIMARY) {
                long currentPrimaryCount = cropExpertiseRepository
                        .countByExpertProfileIdAndExpertiseType(profile.getId(), CropExpertiseType.PRIMARY);
                if (currentPrimaryCount >= MAX_PRIMARY_CROPS) {
                    throw new BadRequestException(
                            "An expert may have at most " + MAX_PRIMARY_CROPS + " primary crops. " +
                            "Please remove one before promoting another to PRIMARY."
                    );
                }
            }
            entry.setExpertiseType(request.expertiseType());
            // Any modification resets verification status to PENDING
            entry.setVerificationStatus(CropExpertiseVerificationStatus.PENDING);
            entry.setVerifiedAt(null);
            entry.setVerifiedBy(null);
        }

        ExpertCropExpertise saved = cropExpertiseRepository.save(entry);
        log.info("Expert {} {} crop expertise for crop '{}' (type={})",
                userId,
                entry.getId() == null ? "added" : "updated",
                crop.getName(),
                request.expertiseType());
        return CropExpertiseResponse.from(saved);
    }

    @Override
    @Transactional
    public void removeCropExpertise(Long userId, Long cropId) {
        ExpertProfile profile = requireProfile(userId);
        ExpertCropExpertise entry = cropExpertiseRepository
                .findByExpertProfileIdAndCropId(profile.getId(), cropId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Crop expertise not found for cropId=" + cropId + " on your profile."));
        cropExpertiseRepository.delete(entry);
        log.info("Expert {} removed crop expertise for cropId={}", userId, cropId);
    }

    // ─── Specializations ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public SpecializationResponse addSpecialization(Long userId, Long specializationId) {
        ExpertProfile profile = requireProfile(userId);

        if (specializationRepository.existsByExpertProfileIdAndSpecializationId(profile.getId(), specializationId)) {
            throw new ConflictException("You have already added this specialization to your profile.");
        }

        Specialization spec = specializationRepo.findById(specializationId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with id: " + specializationId));

        ExpertSpecialization saved = specializationRepository.save(new ExpertSpecialization(profile, spec));
        log.info("Expert {} added specialization '{}'", userId, spec.getName());
        return SpecializationResponse.from(saved);
    }

    @Override
    @Transactional
    public void removeSpecialization(Long userId, Long specializationId) {
        ExpertProfile profile = requireProfile(userId);
        ExpertSpecialization entry = specializationRepository
                .findByExpertProfileIdAndSpecializationId(profile.getId(), specializationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialization not found with id=" + specializationId + " on your profile."));
        specializationRepository.delete(entry);
        log.info("Expert {} removed specializationId={}", userId, specializationId);
    }

    // ─── Locations ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LocationResponse addLocation(Long userId, Long locationId) {
        ExpertProfile profile = requireProfile(userId);

        if (locationRepository.existsByExpertProfileIdAndLocationId(profile.getId(), locationId)) {
            throw new ConflictException("You have already added this location to your profile.");
        }

        Location location = locationRepo.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + locationId));

        ExpertLocation saved = locationRepository.save(new ExpertLocation(profile, location));
        log.info("Expert {} added location '{}'", userId, location.getName());
        return LocationResponse.from(saved);
    }

    @Override
    @Transactional
    public void removeLocation(Long userId, Long locationId) {
        ExpertProfile profile = requireProfile(userId);
        ExpertLocation entry = locationRepository
                .findByExpertProfileIdAndLocationId(profile.getId(), locationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Location not found with id=" + locationId + " on your profile."));
        locationRepository.delete(entry);
        log.info("Expert {} removed locationId={}", userId, locationId);
    }

    // ─── Verification Application ─────────────────────────────────────────────

    @Override
    @Transactional
    public ExpertProfileResponse submitApplication(Long userId) {
        ExpertProfile profile = requireProfile(userId);
        profile.submitApplication(); // throws IllegalStateException if invalid transition
        ExpertProfile saved = profileRepository.save(profile);
        log.info("Expert {} submitted verification application", userId);
        return ExpertProfileResponse.from(
                profileRepository.findByUserIdWithDetails(userId).orElse(saved));
    }

    // ─── Verification Documents ─────────────────────────────────────────────

    @Override
    @Transactional
    public ExpertDocumentResponse saveDocument(Long userId, SaveExpertDocumentRequest request) {
        ExpertProfile profile = requireProfile(userId);
        documentRepository.deleteByExpertProfileIdAndDocumentType(profile.getId(), request.documentType());

        ExpertDocument doc = new ExpertDocument(
                profile,
                request.documentType(),
                request.title(),
                request.fileName(),
                request.fileType(),
                request.fileSize(),
                request.fileUrl()
        );
        ExpertDocument saved = documentRepository.save(doc);
        log.info("Expert {} saved verification document: type={}, file={}", userId, request.documentType(), request.fileName());
        return ExpertDocumentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<ExpertDocumentResponse> getDocuments(Long userId) {
        ExpertProfile profile = requireProfile(userId);
        return documentRepository.findByExpertProfileId(profile.getId()).stream()
                .map(ExpertDocumentResponse::from)
                .toList();
    }

    // ─── Internal Helpers ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ExpertProfile ensureProfileExists(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> createProfileForUser(userId));
    }

    private ExpertProfile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> createProfileForUser(userId));
    }

    private ExpertProfile createProfileForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        ExpertProfile profile = ExpertProfile.createFor(user);
        ExpertProfile saved = profileRepository.save(profile);
        log.info("Auto-created ExpertProfile for user {}", userId);
        return saved;
    }
}
