package com.krishiai.location.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.location.dto.LocationResponse;
import com.krishiai.location.entity.LocationType;
import com.krishiai.location.service.LocationService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public read-only API for browsing the geographic location tree.
 *
 * <p>Base path: {@code /api/v1/locations}
 */
@RestController
@RequestMapping("/api/v1/locations")
@Validated
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * GET /api/v1/locations
     * Returns all active locations. Optional {@code type} filter.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getLocations(
            @RequestParam(required = false) LocationType type) {

        List<LocationResponse> locations = (type != null)
                ? locationService.getLocationsByType(type)
                : locationService.getAllActiveLocations();

        return ResponseEntity.ok(ApiResponse.success("Locations retrieved successfully", locations));
    }

    /**
     * GET /api/v1/locations/roots
     * Returns only top-level (root) locations.
     */
    @GetMapping("/roots")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getRootLocations() {
        return ResponseEntity.ok(ApiResponse.success(
                "Root locations retrieved successfully",
                locationService.getRootLocations()));
    }

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(
                "Provinces retrieved successfully",
                locationService.getProvinces()));
    }

    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getDistrictsByProvince(
            @PathVariable @Positive Long provinceId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Districts retrieved successfully",
                locationService.getDistrictsByProvince(provinceId)));
    }

    @GetMapping("/districts/{districtId}/municipalities")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getMunicipalitiesByDistrict(
            @PathVariable @Positive Long districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Municipalities retrieved successfully",
                locationService.getMunicipalitiesByDistrict(districtId)));
    }

    /**
     * GET /api/v1/locations/{parentId}/children
     * Returns direct children of the given parent location.
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getChildren(
            @PathVariable @Positive Long parentId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Child locations retrieved successfully",
                locationService.getChildLocations(parentId)));
    }

    /**
     * GET /api/v1/locations/{locationId}
     * Returns detail of the specified location.
     */
    @GetMapping("/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationById(
            @PathVariable @Positive Long locationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Location retrieved successfully",
                locationService.getLocationById(locationId)));
    }
}
