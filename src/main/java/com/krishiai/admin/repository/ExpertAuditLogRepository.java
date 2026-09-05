package com.krishiai.admin.repository;

import com.krishiai.admin.entity.ExpertAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertAuditLogRepository extends JpaRepository<ExpertAuditLog, Long> {

    List<ExpertAuditLog> findByTargetProfileIdOrderByCreatedAtDesc(Long profileId);
}
