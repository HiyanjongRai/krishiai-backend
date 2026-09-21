package com.krishiai.consultation.repository;

import com.krishiai.consultation.entity.Consultation;
import com.krishiai.consultation.entity.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByFarmerId(Long farmerId);

    List<Consultation> findByExpertId(Long expertId);

    List<Consultation> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);

    List<Consultation> findByExpertIdOrderByCreatedAtDesc(Long expertId);

    List<Consultation> findByFarmerIdAndStatusOrderByCreatedAtDesc(Long farmerId, ConsultationStatus status);

    List<Consultation> findByExpertIdAndStatusOrderByCreatedAtDesc(Long expertId, ConsultationStatus status);

    Page<Consultation> findByStatus(ConsultationStatus status, Pageable pageable);

    Page<Consultation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM Consultation c " +
           "LEFT JOIN FETCH c.farmer " +
           "LEFT JOIN FETCH c.expert " +
           "LEFT JOIN FETCH c.crop " +
           "LEFT JOIN FETCH c.packageEntity " +
           "WHERE c.id = :id")
    Optional<Consultation> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT c FROM Consultation c WHERE c.status = 'ACTIVE' AND c.expiresAt IS NOT NULL AND c.expiresAt < :now")
    List<Consultation> findActiveExpiredConsultations(@Param("now") java.time.LocalDateTime now);

    long countByStatus(ConsultationStatus status);

    long countByFarmerIdAndStatus(Long farmerId, ConsultationStatus status);

    long countByExpertIdAndStatus(Long expertId, ConsultationStatus status);

}
