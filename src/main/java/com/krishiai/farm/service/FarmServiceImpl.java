package com.krishiai.farm.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.farm.dto.CreateFarmRequest;
import com.krishiai.farm.dto.FarmResponse;
import com.krishiai.farm.dto.UpdateFarmRequest;
import com.krishiai.farm.entity.Farm;
import com.krishiai.farm.repository.FarmRepository;
import com.krishiai.location.entity.Location;
import com.krishiai.location.service.LocationService;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmServiceImpl implements FarmService {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final LocationService locationService;

    @Override
    @Transactional
    public FarmResponse createFarm(Long farmerUserId, CreateFarmRequest request) {
        User farmer = getValidatedFarmer(farmerUserId);

        String trimmedName = request.farmName().strip();
        if (farmRepository.existsByFarmerIdAndFarmNameIgnoreCaseAndActiveTrue(farmer.getId(), trimmedName)) {
            throw new ConflictException("You already have a farm registered with the name '" + trimmedName + "'");
        }

        Location location = locationService.getLocationEntity(request.locationId());

        Farm farm = new Farm(
                farmer,
                trimmedName,
                location,
                request.area(),
                request.areaUnit(),
                request.farmType(),
                request.description() != null ? request.description().strip() : null,
                request.latitude(),
                request.longitude()
        );

        Farm savedFarm = farmRepository.save(farm);
        log.info("Farm '{}' (id={}) created successfully for farmerId={}", savedFarm.getFarmName(), savedFarm.getId(), farmerUserId);
        return FarmResponse.from(savedFarm);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getMyFarm(Long farmerUserId) {
        getValidatedFarmer(farmerUserId);
        Farm farm = farmRepository.findFirstByFarmerIdAndActiveTrueOrderByIdAsc(farmerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No active farm profile found for this farmer. Please create one."));
        return FarmResponse.from(farm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmResponse> getAllMyFarms(Long farmerUserId) {
        getValidatedFarmer(farmerUserId);
        return farmRepository.findByFarmerIdAndActiveTrue(farmerUserId)
                .stream()
                .map(FarmResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getMyFarmById(Long farmerUserId, Long farmId) {
        getValidatedFarmer(farmerUserId);
        Farm farm = farmRepository.findByIdAndFarmerIdAndActiveTrue(farmId, farmerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));
        return FarmResponse.from(farm);
    }

    @Override
    @Transactional
    public FarmResponse updateMyFarm(Long farmerUserId, UpdateFarmRequest request) {
        getValidatedFarmer(farmerUserId);
        Farm farm = farmRepository.findFirstByFarmerIdAndActiveTrueOrderByIdAsc(farmerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No active farm profile found to update"));
        return applyUpdate(farm, request);
    }

    @Override
    @Transactional
    public FarmResponse updateMyFarmById(Long farmerUserId, Long farmId, UpdateFarmRequest request) {
        getValidatedFarmer(farmerUserId);
        Farm farm = farmRepository.findByIdAndFarmerIdAndActiveTrue(farmId, farmerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));
        return applyUpdate(farm, request);
    }

    @Override
    @Transactional
    public void deleteMyFarm(Long farmerUserId) {
        getValidatedFarmer(farmerUserId);
        Farm farm = farmRepository.findFirstByFarmerIdAndActiveTrueOrderByIdAsc(farmerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No active farm found to delete"));
        farm.setActive(false);
        farmRepository.save(farm);
        log.info("Farm id={} soft-deleted for farmerId={}", farm.getId(), farmerUserId);
    }

    @Override
    @Transactional
    public void deleteMyFarmById(Long farmerUserId, Long farmId) {
        getValidatedFarmer(farmerUserId);
        Farm farm = farmRepository.findByIdAndFarmerIdAndActiveTrue(farmId, farmerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));
        farm.setActive(false);
        farmRepository.save(farm);
        log.info("Farm id={} soft-deleted for farmerId={}", farm.getId(), farmerUserId);
    }

    private FarmResponse applyUpdate(Farm farm, UpdateFarmRequest request) {
        if (request.farmName() != null && !request.farmName().isBlank()) {
            String cleanName = request.farmName().strip();
            if (!cleanName.equalsIgnoreCase(farm.getFarmName()) &&
                    farmRepository.existsByFarmerIdAndFarmNameIgnoreCaseAndActiveTrue(farm.getFarmer().getId(), cleanName)) {
                throw new ConflictException("You already have a farm registered with the name '" + cleanName + "'");
            }
            farm.setFarmName(cleanName);
        }

        if (request.locationId() != null) {
            Location location = locationService.getLocationEntity(request.locationId());
            farm.setLocation(location);
        }

        if (request.area() != null) {
            farm.setArea(request.area());
        }

        if (request.areaUnit() != null) {
            farm.setAreaUnit(request.areaUnit());
        }

        if (request.farmType() != null) {
            farm.setFarmType(request.farmType());
        }

        if (request.description() != null) {
            farm.setDescription(request.description().strip());
        }

        if (request.latitude() != null) {
            farm.setLatitude(request.latitude());
        }

        if (request.longitude() != null) {
            farm.setLongitude(request.longitude());
        }

        Farm updated = farmRepository.save(farm);
        log.info("Farm id={} updated successfully", updated.getId());
        return FarmResponse.from(updated);
    }

    private User getValidatedFarmer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != UserRole.ROLE_FARMER) {
            throw new ForbiddenException("Only registered farmers can access farm management");
        }

        if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Account is " + user.getStatus() + ". Farm management disabled.");
        }

        return user;
    }
}
