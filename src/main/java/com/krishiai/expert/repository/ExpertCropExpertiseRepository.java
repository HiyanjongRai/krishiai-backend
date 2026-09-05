package com.krishiai.expert.repository;

import com.krishiai.expert.entity.CropExpertiseType;
import com.krishiai.expert.entity.ExpertCropExpertise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExpertCropExpertiseRepository extends JpaRepository<ExpertCropExpertise, Long> {

    /**
     * Count the number of PRIMARY-type crop expertises for an expert.
     * Used to enforce the "max 3 primary crops" business rule.
     */
    @Query("""
            SELECT COUNT(ece) FROM ExpertCropExpertise ece
            WHERE ece.expertProfile.id = :profileId
              AND ece.expertiseType = :type
            """)
    long countByExpertProfileIdAndExpertiseType(
            @Param("profileId") Long profileId,
            @Param("type") CropExpertiseType type
    );

    Optional<ExpertCropExpertise> findByExpertProfileIdAndCropId(Long profileId, Long cropId);

    boolean existsByExpertProfileIdAndCropId(Long profileId, Long cropId);
}
