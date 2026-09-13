package com.krishiai.consultation.repository;

import com.krishiai.consultation.entity.ConsultationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, Long> {

    List<ConsultationMessage> findByConsultationIdOrderBySentAtAsc(Long consultationId);
}
