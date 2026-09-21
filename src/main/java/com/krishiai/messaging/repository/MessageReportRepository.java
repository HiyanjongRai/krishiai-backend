package com.krishiai.messaging.repository;

import com.krishiai.messaging.entity.MessageReport;
import com.krishiai.messaging.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageReportRepository extends JpaRepository<MessageReport, Long> {

    @Query("SELECT mr FROM MessageReport mr JOIN FETCH mr.reportedBy WHERE mr.status = :status ORDER BY mr.createdAt DESC")
    List<MessageReport> findByStatusWithReporter(ReportStatus status);

    @Query(value = "SELECT mr FROM MessageReport mr JOIN FETCH mr.reportedBy ORDER BY mr.createdAt DESC",
           countQuery = "SELECT count(mr) FROM MessageReport mr")
    Page<MessageReport> findAllWithReporter(Pageable pageable);

    boolean existsByMessageIdAndReportedById(Long messageId, Long reportedById);
}
