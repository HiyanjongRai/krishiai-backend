package com.krishiai.location.dto;

import com.krishiai.location.entity.Location;
import com.krishiai.location.entity.LocationType;

public record LocationResponse(
        Long id,
        String name,
        String nepaliName,
        LocationType type,
        Long parentId,
        String parentName
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getNepaliName(),
                location.getType(),
                location.getParent() != null ? location.getParent().getId() : null,
                location.getParent() != null ? location.getParent().getName() : null
        );
    }
}
