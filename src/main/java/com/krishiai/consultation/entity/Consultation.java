package com.krishiai.consultation.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.crop.entity.Crop;
import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(
        name = "consultations",
        indexes = {
                @Index(name = "idx_consultations_farmer", columnList = "farmer_id"),
                @Index(name = "idx_consultations_expert", columnList = "expert_id"),
                @Index(name = "idx_consultations_status", columnList = "status")
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ConsultationStatus status = ConsultationStatus.PENDING;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public Consultation(User farmer, User expert, Crop crop, String title, String description) {
        this.farmer = farmer;
        this.expert = expert;
        this.crop = crop;
        this.title = title;
        this.description = description;
        this.status = ConsultationStatus.PENDING;
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
