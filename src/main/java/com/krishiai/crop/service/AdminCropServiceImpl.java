package com.krishiai.crop.service;

import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.common.response.PageResponse;
import com.krishiai.crop.dto.CreateCropRequest;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.dto.UpdateCropRequest;
import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.entity.CropCategory;
import com.krishiai.crop.repository.CropCategoryRepository;
import com.krishiai.crop.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCropServiceImpl implements AdminCropService {

    private final CropRepository cropRepository;
    private final CropCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CropResponse> getCrops(Long categoryId, String search, Boolean activeOnly, Pageable pageable) {
        String cleanSearch = search != null && !search.isBlank() ? search.strip().toLowerCase() : null;
        boolean onlyActive = Boolean.TRUE.equals(activeOnly);
        Page<Crop> page;
        if (cleanSearch == null) {
            if (onlyActive) {
                page = categoryId == null
                        ? cropRepository.findAllByActiveTrueOrderByNameAsc(pageable)
                        : cropRepository.findByCategoryIdAndActiveTrueOrderByNameAsc(categoryId, pageable);
            } else {
                page = categoryId == null
                        ? cropRepository.findAllByOrderByNameAsc(pageable)
                        : cropRepository.findByCategoryIdOrderByNameAsc(categoryId, pageable);
            }
        } else {
            page = cropRepository.searchAdminCrops(categoryId, cleanSearch, onlyActive, pageable);
        }
        return PageResponse.from(page.map(CropResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public CropResponse getCropById(Long cropId) {
        return CropResponse.from(getCrop(cropId));
    }

    @Override
    @Transactional
    public CropResponse createCrop(CreateCropRequest request) {
        String name = requireText(request.name(), "Crop name is required");
        if (cropRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Crop with name '" + name + "' already exists");
        }

        CropCategory category = getCategory(request.categoryId());
        Crop crop = new Crop();
        crop.setCategory(category);
        crop.setName(name);
        applyOptionalFields(crop, request.scientificName(), request.nepaliName(), request.emoji(), request.imageUrl(), request.description());
        crop.setActive(request.active() == null || request.active());
        crop.setDefaultCrop(false);

        Crop saved = cropRepository.save(crop);
        log.info("Created crop id={} name='{}'", saved.getId(), saved.getName());
        return CropResponse.from(saved);
    }

    @Override
    @Transactional
    public CropResponse updateCrop(Long cropId, UpdateCropRequest request) {
        Crop crop = getCrop(cropId);

        if (request.categoryId() != null) {
            crop.setCategory(getCategory(request.categoryId()));
        }

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().strip();
            cropRepository.findByNameIgnoreCase(name)
                    .filter(existing -> !existing.getId().equals(crop.getId()))
                    .ifPresent(existing -> {
                        throw new ConflictException("Crop with name '" + name + "' already exists");
                    });
            crop.setName(name);
        }

        applyOptionalFields(crop, request.scientificName(), request.nepaliName(), request.emoji(), request.imageUrl(), request.description());
        if (request.active() != null) {
            crop.setActive(request.active());
        }

        Crop saved = cropRepository.save(crop);
        log.info("Updated crop id={}", saved.getId());
        return CropResponse.from(saved);
    }

    @Override
    @Transactional
    public CropResponse updateStatus(Long cropId, boolean active) {
        Crop crop = getCrop(cropId);
        crop.setActive(active);
        return CropResponse.from(cropRepository.save(crop));
    }

    @Override
    @Transactional
    public void deleteCrop(Long cropId) {
        Crop crop = getCrop(cropId);
        crop.setActive(false);
        cropRepository.save(crop);
        log.info("Crop id={} marked inactive (soft delete)", cropId);
    }

    private Crop getCrop(Long cropId) {
        return cropRepository.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + cropId));
    }

    private CropCategory getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop category not found with id: " + categoryId));
    }

    private void applyOptionalFields(Crop crop, String scientificName, String nepaliName, String emoji, String imageUrl, String description) {
        if (scientificName != null) crop.setScientificName(blankToNull(scientificName));
        if (nepaliName != null) crop.setNepaliName(blankToNull(nepaliName));
        if (emoji != null) crop.setEmoji(blankToNull(emoji));
        if (imageUrl != null) crop.setImageUrl(blankToNull(imageUrl));
        if (description != null) crop.setDescription(blankToNull(description));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new com.krishiai.common.exception.BadRequestException(message);
        }
        return value.strip();
    }
}
