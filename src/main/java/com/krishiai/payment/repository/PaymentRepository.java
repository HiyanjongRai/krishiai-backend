package com.krishiai.payment.repository;

import com.krishiai.payment.entity.Payment;
import com.krishiai.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionUuid(String transactionUuid);

    Optional<Payment> findByConsultationIdAndStatus(Long consultationId, PaymentStatus status);

    List<Payment> findByConsultationIdOrderByCreatedAtDesc(Long consultationId);

    List<Payment> findByPayerIdOrderByCreatedAtDesc(Long payerId);

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.consultation c " +
           "JOIN FETCH p.payer " +
           "LEFT JOIN FETCH c.expert " +
           "WHERE p.id = :id")
    Optional<Payment> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.consultation c " +
           "JOIN FETCH p.payer " +
           "LEFT JOIN FETCH c.expert " +
           "WHERE p.transactionUuid = :transactionUuid")
    Optional<Payment> findByTransactionUuidWithDetails(@Param("transactionUuid") String transactionUuid);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal sumTotalSuccessfulPayments();

    @Query("SELECT COALESCE(SUM(p.platformCommission), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal sumTotalPlatformCommission();

    @Query("SELECT COALESCE(SUM(p.expertAmount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.consultation.expert.id = :expertId")
    BigDecimal sumExpertEarningsByExpertId(@Param("expertId") Long expertId);

    long countByStatus(PaymentStatus status);
}
