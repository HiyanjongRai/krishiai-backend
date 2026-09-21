package com.krishiai.payment.entity;

import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "wallet_ledger_entries",
        indexes = {
                @Index(name = "idx_wle_user_type", columnList = "user_id, type"),
                @Index(name = "idx_wle_ref", columnList = "reference_type, reference_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WalletLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Associated user (e.g. expert receiving earning). NULL indicates KrishiAI platform account. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_wle_user"))
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private LedgerEntryType type;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "NPR";

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletLedgerEntry(User user, LedgerEntryType type, BigDecimal amount, String referenceType, Long referenceId, String description) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.currency = "NPR";
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WalletLedgerEntry that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
