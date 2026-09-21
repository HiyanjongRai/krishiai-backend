package com.krishiai.consultation.repository;

import com.krishiai.consultation.entity.ConsultationPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationPackageRepository extends JpaRepository<ConsultationPackage, Long> {

    List<ConsultationPackage> findByExpertIdOrderByCreatedAtDesc(Long expertId);

    List<ConsultationPackage> findByExpertIdAndActiveTrueOrderByPriceAsc(Long expertId);

    @Query("SELECT cp FROM ConsultationPackage cp " +
           "LEFT JOIN FETCH cp.crop " +
           "JOIN FETCH cp.expert " +
           "WHERE cp.id = :id")
    Optional<ConsultationPackage> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT cp FROM ConsultationPackage cp " +
           "LEFT JOIN FETCH cp.crop " +
           "WHERE cp.expert.id = :expertId AND cp.active = true")
    List<ConsultationPackage> findActivePackagesForExpertWithCrop(@Param("expertId") Long expertId);
}
