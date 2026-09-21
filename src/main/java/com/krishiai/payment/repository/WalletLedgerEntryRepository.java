package com.krishiai.payment.repository;

import com.krishiai.payment.entity.LedgerEntryType;
import com.krishiai.payment.entity.WalletLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, Long> {

    List<WalletLedgerEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WalletLedgerEntry w WHERE w.user.id = :userId AND w.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") LedgerEntryType type);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WalletLedgerEntry w WHERE w.type = :type")
    BigDecimal sumAmountByType(@Param("type") LedgerEntryType type);
}
