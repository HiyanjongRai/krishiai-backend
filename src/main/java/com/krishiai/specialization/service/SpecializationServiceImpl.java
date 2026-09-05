package com.krishiai.specialization.service;

import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.specialization.dto.SpecializationResponse;
import com.krishiai.specialization.entity.Specialization;
import com.krishiai.specialization.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecializationServiceImpl implements SpecializationService {

    private final SpecializationRepository specializationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SpecializationResponse> getAllActiveSpecializations() {
        return specializationRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(SpecializationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Specialization getSpecializationEntity(Long id) {
        return specializationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with id: " + id));
    }
}
