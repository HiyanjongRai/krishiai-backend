package com.krishiai.farm.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.location.entity.Location;
import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(
        name = "farms",
        indexes = {
                @Index(name = "idx_farms_farmer_id", columnList = "farmer_id"),
                @Index(name = "idx_farms_location_id", columnList = "location_id"),
                @Index(name = "idx_farms_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Farm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_farms_farmer"))
    private User farmer;

    @NotBlank
    @Size(max = 150)
    @Column(name = "farm_name", nullable = false, length = 150)
    private String farmName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", foreignKey = @ForeignKey(name = "fk_farms_location"))
    private Location location;

    @NotNull
    @Positive
    @Column(name = "area", nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "area_unit", nullable = false, length = 30)
    private FarmAreaUnit areaUnit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "farm_type", nullable = false, length = 30)
    private FarmType farmType;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Column(name = "latitude")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Farm(User farmer, String farmName, Location location, BigDecimal area,
                FarmAreaUnit areaUnit, FarmType farmType, String description,
                Double latitude, Double longitude) {
        this.farmer = farmer;
        this.farmName = farmName;
        this.location = location;
        this.area = area;
        this.areaUnit = areaUnit;
        this.farmType = farmType;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Farm that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
