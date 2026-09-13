package com.krishiai.consultation.repository;

import com.krishiai.consultation.entity.Consultation;
import com.krishiai.consultation.entity.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByFarmerId(Long farmerId);

    List<Consultation> findByExpertId(Long expertId);

    Page<Consultation> findByStatus(ConsultationStatus status, Pageable pageable);

    long countByStatus(ConsultationStatus status);

    long countByFarmerIdAndStatus(Long farmerId, ConsultationStatus status);
}
