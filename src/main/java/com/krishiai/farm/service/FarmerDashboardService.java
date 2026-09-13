package com.krishiai.farm.service;

import com.krishiai.farm.dto.FarmerDashboardResponse;

public interface FarmerDashboardService {

    FarmerDashboardResponse getDashboard(Long farmerUserId);
}
