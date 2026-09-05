package com.krishiai.location.service;

import com.krishiai.location.dto.LocationResponse;
import com.krishiai.location.entity.LocationType;

import java.util.List;

public interface LocationService {

    /** Returns all active root-level locations (no parent). */
    List<LocationResponse> getRootLocations();

    /** Returns all active locations of the given type. */
    List<LocationResponse> getLocationsByType(LocationType type);

    /** Returns all active children of the given parent location. */
    List<LocationResponse> getChildLocations(Long parentId);

    /** Returns all active locations (full flat list). */
    List<LocationResponse> getAllActiveLocations();
}
