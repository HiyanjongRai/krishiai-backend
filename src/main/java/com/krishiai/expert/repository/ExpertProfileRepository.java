package com.krishiai.expert.repository;

import com.krishiai.expert.entity.ExpertApplicationStatus;
import com.krishiai.expert.entity.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long> {

    Optional<ExpertProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByApplicationStatus(ExpertApplicationStatus status);

    long countByVerifiedExpertTrue();

    /**
     * Fetch a profile with all relationships eagerly loaded in a single query.
     * Use this when building a full profile response DTO to avoid N+1 issues.
     */
    @Query("""
            SELECT ep FROM ExpertProfile ep
            JOIN FETCH ep.user u
            WHERE ep.user.id = :userId
            """)
    Optional<ExpertProfile> findByUserIdWithDetails(@Param("userId") Long userId);

    /**
     * Fetch pending expert verification applications with user for admin review.
     */
    @Query("""
            SELECT ep FROM ExpertProfile ep
            JOIN FETCH ep.user u
            WHERE ep.applicationStatus = :status
            ORDER BY ep.submittedAt DESC
            """)
    List<ExpertProfile> findPendingApplicationsWithDetails(@Param("status") ExpertApplicationStatus status);

    /**
     * Fetch all expert profiles with user details, ordered by most recently created.
     */
    @Query("""
            SELECT ep FROM ExpertProfile ep
            JOIN FETCH ep.user u
            ORDER BY u.createdAt DESC
            """)
    List<ExpertProfile> findAllWithUserDetails();

    /**
     * Fetch a single profile by its own ID, with user eagerly loaded.
     */
    @Query("""
            SELECT ep FROM ExpertProfile ep
            JOIN FETCH ep.user u
            WHERE ep.id = :profileId
            """)
    Optional<ExpertProfile> findByIdWithUser(@Param("profileId") Long profileId);

    /**
     * Finds active, verified experts with verified crop expertise for a specified crop name.
     */
    @Query("""
            SELECT DISTINCT ep FROM ExpertProfile ep
            JOIN FETCH ep.user u
            JOIN ep.cropExpertises ece
            JOIN ece.crop c
            WHERE u.status = com.krishiai.user.entity.UserStatus.ACTIVE
              AND ep.verificationStatus = com.krishiai.expert.entity.ExpertVerificationStatus.VERIFIED
              AND ece.verificationStatus = com.krishiai.expert.entity.CropExpertiseVerificationStatus.VERIFIED
              AND LOWER(c.name) = LOWER(:cropName)
            """)
    List<ExpertProfile> findVerifiedExpertsByCropName(@Param("cropName") String cropName);

    /**
     * Finds all active, verified experts.
     */
    @Query("""
            SELECT DISTINCT ep FROM ExpertProfile ep
            JOIN FETCH ep.user u
            WHERE u.status = com.krishiai.user.entity.UserStatus.ACTIVE
              AND ep.verificationStatus = com.krishiai.expert.entity.ExpertVerificationStatus.VERIFIED
            """)
    List<ExpertProfile> findAllVerifiedExperts();
}
