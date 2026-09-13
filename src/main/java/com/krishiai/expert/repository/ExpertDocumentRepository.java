package com.krishiai.expert.repository;

import com.krishiai.expert.entity.ExpertDocument;
import com.krishiai.expert.entity.ExpertDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertDocumentRepository extends JpaRepository<ExpertDocument, Long> {
    List<ExpertDocument> findByExpertProfileId(Long expertProfileId);
    void deleteByExpertProfileIdAndDocumentType(Long expertProfileId, String documentType);
    Page<ExpertDocument> findByStatus(ExpertDocumentStatus status, Pageable pageable);
}
