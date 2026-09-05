package com.krishiai.specialization.entity;

import com.krishiai.common.audit.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(
        name = "specializations",
        indexes = {
                @Index(name = "idx_spec_code", columnList = "code", unique = true),
                @Index(name = "idx_spec_active", columnList = "is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_spec_code", columnNames = "code"),
                @UniqueConstraint(name = "uq_spec_name", columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Specialization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Specialization name is required")
    @Size(max = 120, message = "Specialization name must not exceed 120 characters")
    @Column(name = "name", nullable = false, length = 120, unique = true)
    private String name;

    @NotBlank(message = "Specialization code is required")
    @Size(max = 60, message = "Specialization code must not exceed 60 characters")
    @Column(name = "code", nullable = false, length = 60, unique = true)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Specialization(String name, String code, String description, String icon) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.icon = icon;
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Specialization that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
