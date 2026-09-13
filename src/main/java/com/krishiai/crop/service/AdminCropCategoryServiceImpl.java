package com.krishiai.crop.service;

import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.crop.dto.CreateCropCategoryRequest;
import com.krishiai.crop.dto.CropCategoryResponse;
import com.krishiai.crop.dto.UpdateCropCategoryRequest;
import com.krishiai.crop.entity.CropCategory;
import com.krishiai.crop.repository.CropCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCropCategoryServiceImpl implements AdminCropCategoryService {

    private final CropCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CropCategoryResponse> getAllCategories(Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                    .map(CropCategoryResponse::from)
                    .toList();
        }
        return categoryRepository.findAll().stream()
                .map(CropCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CropCategoryResponse getCategoryById(Long categoryId) {
        CropCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop category not found with id: " + categoryId));
        return CropCategoryResponse.from(category);
    }

    @Override
    @Transactional
    public CropCategoryResponse createCategory(CreateCropCategoryRequest request) {
        String name = request.name().strip();
        String code = request.code().strip().toUpperCase();

        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Crop category with name '" + name + "' already exists");
        }

        if (categoryRepository.existsByCode(code)) {
            throw new ConflictException("Crop category with code '" + code + "' already exists");
        }

        CropCategory category = new CropCategory(
                name,
                code,
                request.description() != null ? request.description().strip() : null,
                request.icon() != null ? request.icon().strip() : null
        );

        CropCategory saved = categoryRepository.save(category);
        log.info("Created crop category id={} code='{}'", saved.getId(), saved.getCode());
        return CropCategoryResponse.from(saved);
    }

    @Override
    @Transactional
    public CropCategoryResponse updateCategory(Long categoryId, UpdateCropCategoryRequest request) {
        CropCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop category not found with id: " + categoryId));

        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().strip();
            if (!name.equalsIgnoreCase(category.getName()) && categoryRepository.existsByName(name)) {
                throw new ConflictException("Crop category with name '" + name + "' already exists");
            }
            category.setName(name);
        }

        if (request.code() != null && !request.code().isBlank()) {
            String code = request.code().strip().toUpperCase();
            if (!code.equalsIgnoreCase(category.getCode()) && categoryRepository.existsByCode(code)) {
                throw new ConflictException("Crop category with code '" + code + "' already exists");
            }
            category.setCode(code);
        }

        if (request.description() != null) {
            category.setDescription(request.description().strip());
        }

        if (request.icon() != null) {
            category.setIcon(request.icon().strip());
        }

        if (request.active() != null) {
            category.setActive(request.active());
        }

        CropCategory saved = categoryRepository.save(category);
        log.info("Updated crop category id={}", saved.getId());
        return CropCategoryResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        CropCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop category not found with id: " + categoryId));

        // Soft delete to protect relational integrity with crops
        category.setActive(false);
        categoryRepository.save(category);
        log.info("Crop category id={} marked as inactive (soft-deleted)", categoryId);
    }
}
