package com.krishiai.expert.repository;

import com.krishiai.expert.entity.ExpertDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertDocumentRepository extends JpaRepository<ExpertDocument, Long> {
    List<ExpertDocument> findByExpertProfileId(Long expertProfileId);
    void deleteByExpertProfileIdAndDocumentType(Long expertProfileId, String documentType);
}
