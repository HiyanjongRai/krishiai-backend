package com.krishiai.expert.repository;

import com.krishiai.expert.entity.CropExpertiseType;
import com.krishiai.expert.entity.CropExpertiseVerificationStatus;
import com.krishiai.expert.entity.ExpertCropExpertise;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    Optional<ExpertCropExpertise> findByExpertProfileIdAndExpertiseAreaIgnoreCase(Long profileId, String expertiseArea);

    List<ExpertCropExpertise> findByExpertProfileId(Long profileId);

    List<ExpertCropExpertise> findByVerificationStatus(CropExpertiseVerificationStatus status);

    List<ExpertCropExpertise> findByVerificationStatusIn(Collection<CropExpertiseVerificationStatus> statuses);

    @Query("""
            SELECT ece FROM ExpertCropExpertise ece
            JOIN FETCH ece.expertProfile ep
            JOIN FETCH ep.user u
            LEFT JOIN FETCH ece.crop c
            LEFT JOIN FETCH ece.evidenceDocument ed
            WHERE ece.verificationStatus IN :statuses
            ORDER BY ece.updatedAt DESC
            """)
    List<ExpertCropExpertise> findByVerificationStatusInWithDetails(
            @Param("statuses") Collection<CropExpertiseVerificationStatus> statuses,
            Pageable pageable
    );

    @Query("""
            SELECT ece FROM ExpertCropExpertise ece
            JOIN FETCH ece.expertProfile ep
            JOIN FETCH ep.user u
            LEFT JOIN FETCH ece.crop c
            LEFT JOIN FETCH ece.evidenceDocument ed
            ORDER BY ece.updatedAt DESC
            """)
    List<ExpertCropExpertise> findAllWithDetails(Pageable pageable);

    boolean existsByExpertProfileIdAndCropId(Long profileId, Long cropId);

    boolean existsByExpertProfileIdAndExpertiseAreaIgnoreCase(Long profileId, String expertiseArea);
}
