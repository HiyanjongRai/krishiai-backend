package com.krishiai.consultation.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.crop.entity.Crop;
import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "consultation_packages",
        indexes = {
                @Index(name = "idx_cp_expert_active", columnList = "expert_id, is_active"),
                @Index(name = "idx_cp_crop_active", columnList = "crop_id, is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConsultationPackage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cp_expert"))
    private User expert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", foreignKey = @ForeignKey(name = "fk_cp_crop"))
    private Crop crop;

    @NotBlank(message = "Package name is required")
    @Size(max = 150, message = "Package name must not exceed 150 characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "100.00", message = "Minimum package fee is NPR 100.00")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "NPR";

    @NotNull(message = "Duration in hours is required")
    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours = 168; // Default 7 days

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public ConsultationPackage(User expert, Crop crop, String name, String description, BigDecimal price, Integer durationHours) {
        this.expert = expert;
        this.crop = crop;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationHours = durationHours != null ? durationHours : 168;
        this.currency = "NPR";
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsultationPackage that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
