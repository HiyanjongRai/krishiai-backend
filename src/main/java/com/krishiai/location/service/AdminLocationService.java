package com.krishiai.location.service;

import com.krishiai.location.dto.CreateLocationRequest;
import com.krishiai.location.dto.LocationResponse;
import com.krishiai.location.dto.UpdateLocationRequest;
import com.krishiai.location.entity.LocationType;

import java.util.List;

public interface AdminLocationService {
    List<LocationResponse> getLocations(LocationType type, Long parentId, Boolean activeOnly);
    LocationResponse createProvince(CreateLocationRequest request);
    LocationResponse updateProvince(Long provinceId, UpdateLocationRequest request);
    void deleteProvince(Long provinceId);
    LocationResponse createDistrict(CreateLocationRequest request);
    LocationResponse updateDistrict(Long districtId, UpdateLocationRequest request);
    void deleteDistrict(Long districtId);
    LocationResponse createMunicipality(CreateLocationRequest request);
    LocationResponse updateMunicipality(Long municipalityId, UpdateLocationRequest request);
    void deleteMunicipality(Long municipalityId);
}
