package com.krishiai.location.dto;

import com.krishiai.location.entity.Location;
import com.krishiai.location.entity.LocationType;
import com.krishiai.location.entity.MunicipalityType;

public record LocationResponse(
        Long id,
        String name,
        String nepaliName,
        String code,
        LocationType type,
        MunicipalityType municipalityType,
        Long parentId,
        String parentName,
        boolean active
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getNepaliName(),
                location.getCode(),
                location.getType(),
                location.getMunicipalityType(),
                location.getParent() != null ? location.getParent().getId() : null,
                location.getParent() != null ? location.getParent().getName() : null,
                location.isActive()
        );
    }
}
