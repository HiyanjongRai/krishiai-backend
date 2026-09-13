package com.krishiai.farm.service;

import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.crop.repository.CropRepository;
import com.krishiai.farm.dto.FarmResponse;
import com.krishiai.farm.dto.FarmerDashboardResponse;
import com.krishiai.farm.entity.Farm;
import com.krishiai.farm.repository.FarmRepository;
import com.krishiai.user.dto.UserResponse;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmerDashboardServiceImpl implements FarmerDashboardService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;

    @Override
    @Transactional(readOnly = true)
    public FarmerDashboardResponse getDashboard(Long farmerUserId) {
        User user = userRepository.findById(farmerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found with id: " + farmerUserId));

        if (user.getRole() != UserRole.ROLE_FARMER) {
            throw new ForbiddenException("Only farmers have access to the farmer dashboard");
        }

        if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Your account is currently " + user.getStatus() + ". Dashboard unavailable.");
        }

        UserResponse userResponse = UserResponse.from(user);

        Farm primaryFarm = farmRepository.findFirstByFarmerIdAndActiveTrueOrderByIdAsc(farmerUserId).orElse(null);
        FarmResponse primaryFarmResponse = primaryFarm != null ? FarmResponse.from(primaryFarm) : null;
        int totalFarms = (int) farmRepository.countByFarmerIdAndActiveTrue(farmerUserId);

        long activeCropCount = cropRepository.findByActiveTrueOrderByNameAsc().size();
        long activeConsultations = 0; // Populated when consultations exist

        List<FarmerDashboardResponse.RecentActivityDto> activities = new ArrayList<>();
        if (primaryFarm != null) {
            activities.add(new FarmerDashboardResponse.RecentActivityDto(
                    "Farm Profile Active",
                    "Primary farm '" + primaryFarm.getFarmName() + "' registered",
                    "Recent",
                    "farm_profile"
            ));
        }

        List<FarmerDashboardResponse.DashboardNotificationDto> notifications = new ArrayList<>();
        if (primaryFarm == null) {
            notifications.add(new FarmerDashboardResponse.DashboardNotificationDto(
                    "notif-1",
                    "Complete your profile by adding your farm details.",
                    "warning",
                    false
            ));
        }

        return new FarmerDashboardResponse(
                userResponse,
                primaryFarmResponse,
                totalFarms,
                activeCropCount,
                activeConsultations,
                activities,
                notifications
        );
    }
}
