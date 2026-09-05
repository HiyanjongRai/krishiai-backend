package com.krishiai.expert.entity;

import com.krishiai.location.entity.Location;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Join table between ExpertProfile and Location.
 * Defines which geographic areas an expert serves or is available in.
 */
@Entity
@Table(
        name = "expert_locations",
        indexes = {
                @Index(name = "idx_el_profile_loc", columnList = "expert_profile_id, location_id", unique = true)
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_el_profile_loc", columnNames = {"expert_profile_id", "location_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpertLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_el_expert_profile"))
    private ExpertProfile expertProfile;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "location_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_el_location"))
    private Location location;

    public ExpertLocation(ExpertProfile expertProfile, Location location) {
        this.expertProfile = expertProfile;
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpertLocation that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
