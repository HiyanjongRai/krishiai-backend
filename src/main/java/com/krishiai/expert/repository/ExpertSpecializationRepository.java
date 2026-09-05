package com.krishiai.expert.repository;

import com.krishiai.expert.entity.ExpertSpecialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertSpecializationRepository extends JpaRepository<ExpertSpecialization, Long> {

    boolean existsByExpertProfileIdAndSpecializationId(Long profileId, Long specializationId);

    Optional<ExpertSpecialization> findByExpertProfileIdAndSpecializationId(Long profileId, Long specializationId);
}
