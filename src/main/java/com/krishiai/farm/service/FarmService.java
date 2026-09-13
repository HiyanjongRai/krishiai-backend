package com.krishiai.farm.service;

import com.krishiai.farm.dto.CreateFarmRequest;
import com.krishiai.farm.dto.FarmResponse;
import com.krishiai.farm.dto.UpdateFarmRequest;

import java.util.List;

public interface FarmService {

    FarmResponse createFarm(Long farmerUserId, CreateFarmRequest request);

    FarmResponse getMyFarm(Long farmerUserId);

    List<FarmResponse> getAllMyFarms(Long farmerUserId);

    FarmResponse getMyFarmById(Long farmerUserId, Long farmId);

    FarmResponse updateMyFarm(Long farmerUserId, UpdateFarmRequest request);

    FarmResponse updateMyFarmById(Long farmerUserId, Long farmId, UpdateFarmRequest request);

    void deleteMyFarm(Long farmerUserId);

    void deleteMyFarmById(Long farmerUserId, Long farmId);
}
