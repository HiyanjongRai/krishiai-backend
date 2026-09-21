package com.krishiai.consultation.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.crop.entity.Crop;
import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "consultations",
        indexes = {
                @Index(name = "idx_consultations_farmer", columnList = "farmer_id"),
                @Index(name = "idx_consultations_expert", columnList = "expert_id"),
                @Index(name = "idx_consultations_status", columnList = "status"),
                @Index(name = "idx_consultations_farmer_status", columnList = "farmer_id, status"),
                @Index(name = "idx_consultations_expert_status", columnList = "expert_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Consultation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_consultations_farmer"))
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", foreignKey = @ForeignKey(name = "fk_consultations_expert"))
    private User expert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", foreignKey = @ForeignKey(name = "fk_consultations_crop"))
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", foreignKey = @ForeignKey(name = "fk_consultations_package"))
    private ConsultationPackage packageEntity;

    @Column(name = "price_at_purchase", precision = 12, scale = 2)
    private java.math.BigDecimal priceAtPurchase;

    @Column(name = "currency", length = 10)
    private String currency = "NPR";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ConsultationStatus status = ConsultationStatus.REQUESTED;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "payment_verified_at")
    private LocalDateTime paymentVerifiedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public Consultation(User farmer, User expert, Crop crop, String subject, String description) {
        this.farmer = farmer;
        this.expert = expert;
        this.crop = crop;
        this.subject = subject;
        this.title = subject;
        this.description = description;
        this.status = ConsultationStatus.REQUESTED;
        this.currency = "NPR";
    }

    public Consultation(User farmer, User expert, Crop crop, ConsultationPackage packageEntity, String subject, String description) {
        this.farmer = farmer;
        this.expert = expert;
        this.crop = crop;
        this.packageEntity = packageEntity;
        if (packageEntity != null) {
            this.priceAtPurchase = packageEntity.getPrice();
            this.currency = packageEntity.getCurrency();
        } else {
            this.currency = "NPR";
        }
        this.subject = subject;
        this.title = subject;
        this.description = description;
        this.status = ConsultationStatus.REQUESTED;
    }

    public boolean isExpired() {
        if (expiresAt == null) return false;
        return LocalDateTime.now().isAfter(expiresAt);
    }


    public String getDisplaySubject() {
        if (subject != null && !subject.isBlank()) return subject;
        if (title != null && !title.isBlank()) return title;
        return "Crop Consultation";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Consultation that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
