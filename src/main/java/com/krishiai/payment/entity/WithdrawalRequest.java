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
        name = "withdrawal_requests",
        indexes = {
                @Index(name = "idx_wr_expert_status", columnList = "expert_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wr_expert"))
    private User expert;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "NPR";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "account_details", columnDefinition = "TEXT")
    private String accountDetails;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by", foreignKey = @ForeignKey(name = "fk_wr_admin"))
    private User processedBy;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    public WithdrawalRequest(User expert, BigDecimal amount, String accountDetails) {
        this.expert = expert;
        this.amount = amount;
        this.currency = "NPR";
        this.accountDetails = accountDetails;
        this.status = WithdrawalStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WithdrawalRequest that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
