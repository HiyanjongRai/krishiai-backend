package com.krishiai.farm.repository;

import com.krishiai.farm.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    List<Farm> findByFarmerIdAndActiveTrue(Long farmerId);

    Optional<Farm> findByIdAndFarmerIdAndActiveTrue(Long id, Long farmerId);

    Optional<Farm> findFirstByFarmerIdAndActiveTrueOrderByIdAsc(Long farmerId);

    boolean existsByFarmerIdAndFarmNameIgnoreCaseAndActiveTrue(Long farmerId, String farmName);

    boolean existsByFarmerIdAndActiveTrue(Long farmerId);

    long countByFarmerIdAndActiveTrue(Long farmerId);
}
