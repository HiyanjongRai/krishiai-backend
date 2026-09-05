package com.krishiai.specialization.service;

import com.krishiai.specialization.dto.SpecializationResponse;
import com.krishiai.specialization.entity.Specialization;

import java.util.List;

public interface SpecializationService {
    List<SpecializationResponse> getAllActiveSpecializations();
    Specialization getSpecializationEntity(Long id);
}
