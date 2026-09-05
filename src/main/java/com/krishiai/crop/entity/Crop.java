package com.krishiai.crop.entity;

import com.krishiai.common.audit.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(
        name = "crops",
        indexes = {
                @Index(name = "idx_crops_name", columnList = "name"),
                @Index(name = "idx_crops_cat_active", columnList = "category_id, is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_crops_name", columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Crop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "Crop category is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_crops_category"))
    private CropCategory category;

    @NotBlank(message = "Crop name is required")
    @Size(max = 120, message = "Crop name must not exceed 120 characters")
    @Column(name = "name", nullable = false, length = 120, unique = true)
    private String name;

    @Size(max = 150, message = "Scientific name must not exceed 150 characters")
    @Column(name = "scientific_name", length = 150)
    private String scientificName;

    @Size(max = 120, message = "Nepali name must not exceed 120 characters")
    @Column(name = "nepali_name", length = 120)
    private String nepaliName;

    @Column(name = "emoji", length = 20)
    private String emoji;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Crop(CropCategory category, String name, String scientificName, String nepaliName, String emoji, String description) {
        this.category = category;
        this.name = name;
        this.scientificName = scientificName;
        this.nepaliName = nepaliName;
        this.emoji = emoji;
        this.description = description;
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Crop other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
