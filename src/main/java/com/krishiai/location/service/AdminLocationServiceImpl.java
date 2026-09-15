package com.krishiai.location.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.location.dto.CreateLocationRequest;
import com.krishiai.location.dto.LocationResponse;
import com.krishiai.location.dto.UpdateLocationRequest;
import com.krishiai.location.entity.Location;
import com.krishiai.location.entity.LocationType;
import com.krishiai.location.entity.MunicipalityType;
import com.krishiai.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLocationServiceImpl implements AdminLocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocations(LocationType type, Long parentId, Boolean activeOnly) {
        List<Location> locations;
        if (parentId != null) {
            locations = Boolean.TRUE.equals(activeOnly)
                    ? locationRepository.findByParentIdAndActiveTrueOrderByNameAsc(parentId)
                    : locationRepository.findByParentIdOrderByNameAsc(parentId);
        } else if (type != null) {
            locations = Boolean.TRUE.equals(activeOnly)
                    ? locationRepository.findByTypeAndActiveTrueOrderByNameAsc(type)
                    : locationRepository.findByTypeOrderByNameAsc(type);
        } else {
            locations = locationRepository.findAll().stream()
                    .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        return locations.stream().map(LocationResponse::from).toList();
    }

    @Override
    @Transactional
    public LocationResponse createProvince(CreateLocationRequest request) {
        Location province = createLocation(request, LocationType.PROVINCE, null);
        return LocationResponse.from(province);
    }

    @Override
    @Transactional
    public LocationResponse updateProvince(Long provinceId, UpdateLocationRequest request) {
        Location province = getLocationOfType(provinceId, LocationType.PROVINCE);
        updateLocation(province, request, null, LocationType.PROVINCE);
        return LocationResponse.from(locationRepository.save(province));
    }

    @Override
    @Transactional
    public void deleteProvince(Long provinceId) {
        softDelete(getLocationOfType(provinceId, LocationType.PROVINCE));
    }

    @Override
    @Transactional
    public LocationResponse createDistrict(CreateLocationRequest request) {
        Location province = getLocationOfType(request.parentId(), LocationType.PROVINCE);
        return LocationResponse.from(createLocation(request, LocationType.DISTRICT, province));
    }

    @Override
    @Transactional
    public LocationResponse updateDistrict(Long districtId, UpdateLocationRequest request) {
        Location district = getLocationOfType(districtId, LocationType.DISTRICT);
        Location province = request.parentId() != null ? getLocationOfType(request.parentId(), LocationType.PROVINCE) : district.getParent();
        updateLocation(district, request, province, LocationType.DISTRICT);
        return LocationResponse.from(locationRepository.save(district));
    }

    @Override
    @Transactional
    public void deleteDistrict(Long districtId) {
        softDelete(getLocationOfType(districtId, LocationType.DISTRICT));
    }

    @Override
    @Transactional
    public LocationResponse createMunicipality(CreateLocationRequest request) {
        Location district = getLocationOfType(request.parentId(), LocationType.DISTRICT);
        Location municipality = createLocation(request, LocationType.MUNICIPALITY, district);
        municipality.setMunicipalityType(request.municipalityType() != null ? request.municipalityType() : MunicipalityType.MUNICIPALITY);
        return LocationResponse.from(locationRepository.save(municipality));
    }

    @Override
    @Transactional
    public LocationResponse updateMunicipality(Long municipalityId, UpdateLocationRequest request) {
        Location municipality = getLocationOfType(municipalityId, LocationType.MUNICIPALITY);
        Location district = request.parentId() != null ? getLocationOfType(request.parentId(), LocationType.DISTRICT) : municipality.getParent();
        updateLocation(municipality, request, district, LocationType.MUNICIPALITY);
        municipality.setMunicipalityType(request.municipalityType() != null ? request.municipalityType() : municipality.getMunicipalityType());
        return LocationResponse.from(locationRepository.save(municipality));
    }

    @Override
    @Transactional
    public void deleteMunicipality(Long municipalityId) {
        softDelete(getLocationOfType(municipalityId, LocationType.MUNICIPALITY));
    }

    private Location createLocation(CreateLocationRequest request, LocationType type, Location parent) {
        String name = requireName(request.name());
        ensureUniqueName(name, parent != null ? parent.getId() : null, type, null);
        ensureUniqueCode(request.code(), null);

        Location location = new Location(parent, name, blankToNull(request.nepaliName()), type);
        location.setCode(normalizeCode(request.code()));
        location.setActive(request.active() == null || request.active());
        Location saved = locationRepository.save(location);
        log.info("Created location id={} type={} name='{}'", saved.getId(), saved.getType(), saved.getName());
        return saved;
    }

    private void updateLocation(Location location, UpdateLocationRequest request, Location parent, LocationType type) {
        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().strip();
            ensureUniqueName(name, parent != null ? parent.getId() : null, type, location.getId());
            location.setName(name);
        }
        if (request.nepaliName() != null) {
            location.setNepaliName(blankToNull(request.nepaliName()));
        }
        if (request.code() != null) {
            ensureUniqueCode(request.code(), location.getId());
            location.setCode(normalizeCode(request.code()));
        }
        location.setParent(parent);
        if (request.active() != null) {
            location.setActive(request.active());
        }
    }

    private void softDelete(Location location) {
        location.setActive(false);
        locationRepository.save(location);
        log.info("Location id={} marked inactive", location.getId());
    }

    private Location getLocationOfType(Long id, LocationType type) {
        if (id == null) {
            throw new BadRequestException(type + " id is required");
        }
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));
        if (location.getType() != type) {
            throw new BadRequestException("Location " + id + " must be a " + type);
        }
        return location;
    }

    private void ensureUniqueName(String name, Long parentId, LocationType type, Long currentId) {
        locationRepository.findByNameAndParentAndTypeIgnoreCase(name, parentId, type)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ConflictException(type + " named '" + name + "' already exists under this parent");
                });
    }

    private void ensureUniqueCode(String code, Long currentId) {
        String normalized = normalizeCode(code);
        if (normalized == null) return;
        locationRepository.findByCodeIgnoreCase(normalized)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ConflictException("Location code '" + normalized + "' already exists");
                });
    }

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Location name is required");
        }
        return value.strip();
    }

    private String normalizeCode(String value) {
        return value == null || value.isBlank() ? null : value.strip().toUpperCase().replaceAll("\\s+", "_");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
