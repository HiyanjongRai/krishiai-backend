package com.krishiai.specialization.dto;

import com.krishiai.specialization.entity.Specialization;

public record SpecializationResponse(
        Long id,
        String name,
        String code,
        String description,
        String icon,
        boolean active
) {
    public static SpecializationResponse from(Specialization specialization) {
        return new SpecializationResponse(
                specialization.getId(),
                specialization.getName(),
                specialization.getCode(),
                specialization.getDescription(),
                specialization.getIcon(),
                specialization.isActive()
        );
    }
}
