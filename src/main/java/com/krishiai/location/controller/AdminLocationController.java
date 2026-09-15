package com.krishiai.location.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.location.dto.CreateLocationRequest;
import com.krishiai.location.dto.LocationResponse;
import com.krishiai.location.dto.UpdateLocationRequest;
import com.krishiai.location.entity.LocationType;
import com.krishiai.location.service.AdminLocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/locations")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Validated
@RequiredArgsConstructor
public class AdminLocationController {

    private final AdminLocationService adminLocationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getLocations(
            @RequestParam(required = false) LocationType type,
            @RequestParam(required = false) @Positive Long parentId,
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                "Locations retrieved successfully",
                adminLocationService.getLocations(type, parentId, activeOnly)));
    }

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getProvinces(
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                "Provinces retrieved successfully",
                adminLocationService.getLocations(LocationType.PROVINCE, null, activeOnly)));
    }

    @PostMapping("/provinces")
    public ResponseEntity<ApiResponse<LocationResponse>> createProvince(@Valid @RequestBody CreateLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Province created successfully", adminLocationService.createProvince(request)));
    }

    @PutMapping("/provinces/{provinceId}")
    public ResponseEntity<ApiResponse<LocationResponse>> updateProvince(
            @PathVariable @Positive Long provinceId,
            @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Province updated successfully", adminLocationService.updateProvince(provinceId, request)));
    }

    @DeleteMapping("/provinces/{provinceId}")
    public ResponseEntity<ApiResponse<Void>> deleteProvince(@PathVariable @Positive Long provinceId) {
        adminLocationService.deleteProvince(provinceId);
        return ResponseEntity.ok(ApiResponse.success("Province deactivated successfully"));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getDistricts(
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                "Districts retrieved successfully",
                adminLocationService.getLocations(LocationType.DISTRICT, null, activeOnly)));
    }

    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getDistrictsByProvince(
            @PathVariable @Positive Long provinceId,
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                "Districts retrieved successfully",
                adminLocationService.getLocations(LocationType.DISTRICT, provinceId, activeOnly)));
    }

    @PostMapping("/districts")
    public ResponseEntity<ApiResponse<LocationResponse>> createDistrict(@Valid @RequestBody CreateLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("District created successfully", adminLocationService.createDistrict(request)));
    }

    @PutMapping("/districts/{districtId}")
    public ResponseEntity<ApiResponse<LocationResponse>> updateDistrict(
            @PathVariable @Positive Long districtId,
            @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("District updated successfully", adminLocationService.updateDistrict(districtId, request)));
    }

    @DeleteMapping("/districts/{districtId}")
    public ResponseEntity<ApiResponse<Void>> deleteDistrict(@PathVariable @Positive Long districtId) {
        adminLocationService.deleteDistrict(districtId);
        return ResponseEntity.ok(ApiResponse.success("District deactivated successfully"));
    }

    @GetMapping("/municipalities")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getMunicipalities(
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                "Municipalities retrieved successfully",
                adminLocationService.getLocations(LocationType.MUNICIPALITY, null, activeOnly)));
    }

    @GetMapping("/districts/{districtId}/municipalities")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getMunicipalitiesByDistrict(
            @PathVariable @Positive Long districtId,
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                "Municipalities retrieved successfully",
                adminLocationService.getLocations(LocationType.MUNICIPALITY, districtId, activeOnly)));
    }

    @PostMapping("/municipalities")
    public ResponseEntity<ApiResponse<LocationResponse>> createMunicipality(@Valid @RequestBody CreateLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Municipality created successfully", adminLocationService.createMunicipality(request)));
    }

    @PutMapping("/municipalities/{municipalityId}")
    public ResponseEntity<ApiResponse<LocationResponse>> updateMunicipality(
            @PathVariable @Positive Long municipalityId,
            @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Municipality updated successfully", adminLocationService.updateMunicipality(municipalityId, request)));
    }

    @DeleteMapping("/municipalities/{municipalityId}")
    public ResponseEntity<ApiResponse<Void>> deleteMunicipality(@PathVariable @Positive Long municipalityId) {
        adminLocationService.deleteMunicipality(municipalityId);
        return ResponseEntity.ok(ApiResponse.success("Municipality deactivated successfully"));
    }
}
