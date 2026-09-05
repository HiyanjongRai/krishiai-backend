package com.krishiai.crop.entity;

import com.krishiai.common.audit.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "crop_categories",
        indexes = {
                @Index(name = "idx_crop_cat_code", columnList = "code", unique = true),
                @Index(name = "idx_crop_cat_active", columnList = "is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_crop_cat_code", columnNames = "code"),
                @UniqueConstraint(name = "uq_crop_cat_name", columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CropCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @NotBlank(message = "Category code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Crop> crops = new ArrayList<>();

    public CropCategory(String name, String code, String description, String icon) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.icon = icon;
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropCategory that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
