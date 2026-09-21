package com.krishiai.payment.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.consultation.entity.Consultation;
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
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_consultation", columnList = "consultation_id"),
                @Index(name = "idx_payments_payer", columnList = "payer_id"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_provider_ref", columnList = "provider, provider_reference_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_payment_transaction_uuid", columnNames = "transaction_uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payments_consultation"))
    private Consultation consultation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payments_payer"))
    private User payer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider = PaymentProvider.ESEWA;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "provider_reference_id", length = 100)
    private String providerReferenceId;

    @NotNull
    @Column(name = "transaction_uuid", nullable = false, unique = true, length = 100)
    private String transactionUuid;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "NPR";

    @NotNull
    @Column(name = "platform_commission", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformCommission = BigDecimal.ZERO;

    @NotNull
    @Column(name = "expert_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal expertAmount = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    public Payment(Consultation consultation, User payer, PaymentProvider provider, String transactionUuid,
                   BigDecimal amount, BigDecimal platformCommission, BigDecimal expertAmount) {
        this.consultation = consultation;
        this.payer = payer;
        this.provider = provider;
        this.transactionUuid = transactionUuid;
        this.amount = amount;
        this.currency = "NPR";
        this.platformCommission = platformCommission;
        this.expertAmount = expertAmount;
        this.status = PaymentStatus.PENDING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return id != null && id.equals(payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
