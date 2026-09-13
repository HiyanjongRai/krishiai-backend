package com.krishiai.location.service;

import com.krishiai.location.dto.LocationResponse;
import com.krishiai.location.entity.LocationType;
import com.krishiai.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getRootLocations() {
        return locationRepository.findByParentIdAndActiveTrueOrderByNameAsc(null)
                .stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationsByType(LocationType type) {
        return locationRepository.findByTypeAndActiveTrueOrderByNameAsc(type)
                .stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getChildLocations(Long parentId) {
        return locationRepository.findByParentIdAndActiveTrueOrderByNameAsc(parentId)
                .stream()
                .map(LocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllActiveLocations() {
        return locationRepository.findAll()
                .stream()
                .filter(l -> l.isActive())
                .map(LocationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(Long id) {
        return LocationResponse.from(getLocationEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public com.krishiai.location.entity.Location getLocationEntity(Long id) {
        return locationRepository.findById(id)
                .filter(com.krishiai.location.entity.Location::isActive)
                .orElseThrow(() -> new com.krishiai.common.exception.ResourceNotFoundException("Location not found with id: " + id));
    }
}
