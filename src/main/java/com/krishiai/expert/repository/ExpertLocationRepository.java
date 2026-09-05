package com.krishiai.expert.repository;

import com.krishiai.expert.entity.ExpertLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertLocationRepository extends JpaRepository<ExpertLocation, Long> {

    boolean existsByExpertProfileIdAndLocationId(Long profileId, Long locationId);

    Optional<ExpertLocation> findByExpertProfileIdAndLocationId(Long profileId, Long locationId);
}
