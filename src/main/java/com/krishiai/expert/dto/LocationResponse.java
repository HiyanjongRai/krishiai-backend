package com.krishiai.expert.dto;

import com.krishiai.expert.entity.ExpertLocation;

public record LocationResponse(
        Long id,
        Long locationId,
        String name,
        String nepaliName,
        String type
) {
    public static LocationResponse from(ExpertLocation el) {
        return new LocationResponse(
                el.getId(),
                el.getLocation().getId(),
                el.getLocation().getName(),
                el.getLocation().getNepaliName(),
                el.getLocation().getType().name()
        );
    }
}
