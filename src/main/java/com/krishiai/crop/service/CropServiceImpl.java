package com.krishiai.crop.service;

import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.common.response.PageResponse;
import com.krishiai.crop.dto.CropCategoryResponse;
import com.krishiai.crop.dto.CropResponse;
import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.repository.CropCategoryRepository;
import com.krishiai.crop.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CropServiceImpl implements CropService {

    private final CropCategoryRepository categoryRepository;
    private final CropRepository cropRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CropCategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CropCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CropResponse> getActiveCrops(Long categoryId, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.strip().toLowerCase() : null;
        Page<Crop> page;
        if (cleanSearch == null) {
            if (categoryId == null) {
                page = cropRepository.findAllByActiveTrueOrderByNameAsc(pageable);
            } else {
                page = cropRepository.findByCategoryIdAndActiveTrueOrderByNameAsc(categoryId, pageable);
            }
        } else {
            page = cropRepository.searchActiveCrops(categoryId, cleanSearch, pageable);
        }
        return PageResponse.from(page.map(CropResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public CropResponse getCropById(Long id) {
        return CropResponse.from(getCropEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Crop getCropEntity(Long id) {
        return cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + id));
    }
}
