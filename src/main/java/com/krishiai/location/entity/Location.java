package com.krishiai.location.entity;

import com.krishiai.common.audit.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "locations",
        indexes = {
                @Index(name = "idx_locations_parent", columnList = "parent_id, type"),
                @Index(name = "idx_locations_type_active", columnList = "type, is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Location extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_locations_parent"))
    private Location parent;

    @NotBlank(message = "Location name is required")
    @Size(max = 120, message = "Location name must not exceed 120 characters")
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Size(max = 120, message = "Nepali name must not exceed 120 characters")
    @Column(name = "nepali_name", length = 120)
    private String nepaliName;

    @NotNull(message = "Location type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private LocationType type;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Location> children = new ArrayList<>();

    public Location(Location parent, String name, String nepaliName, LocationType type) {
        this.parent = parent;
        this.name = name;
        this.nepaliName = nepaliName;
        this.type = type;
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
