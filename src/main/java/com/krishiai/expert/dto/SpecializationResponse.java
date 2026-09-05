package com.krishiai.expert.dto;

import com.krishiai.expert.entity.ExpertSpecialization;

public record SpecializationResponse(
        Long id,
        Long specializationId,
        String name,
        String code,
        String icon
) {
    public static SpecializationResponse from(ExpertSpecialization es) {
        return new SpecializationResponse(
                es.getId(),
                es.getSpecialization().getId(),
                es.getSpecialization().getName(),
                es.getSpecialization().getCode(),
                es.getSpecialization().getIcon()
        );
    }
}
