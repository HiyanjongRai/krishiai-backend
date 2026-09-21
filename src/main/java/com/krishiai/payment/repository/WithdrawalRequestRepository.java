package com.krishiai.payment.repository;

import com.krishiai.payment.entity.WithdrawalRequest;
import com.krishiai.payment.entity.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    List<WithdrawalRequest> findByExpertIdOrderByRequestedAtDesc(Long expertId);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(WithdrawalStatus status);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequest w WHERE w.expert.id = :expertId AND w.status = 'COMPLETED'")
    BigDecimal sumCompletedWithdrawalsByExpertId(@Param("expertId") Long expertId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequest w WHERE w.expert.id = :expertId AND w.status IN ('PENDING', 'APPROVED', 'PROCESSING')")
    BigDecimal sumPendingWithdrawalsByExpertId(@Param("expertId") Long expertId);
}
