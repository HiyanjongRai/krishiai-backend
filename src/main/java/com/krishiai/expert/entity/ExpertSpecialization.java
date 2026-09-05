package com.krishiai.expert.entity;

import com.krishiai.specialization.entity.Specialization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Join table between ExpertProfile and Specialization.
 * An expert may have multiple professional specializations.
 */
@Entity
@Table(
        name = "expert_specializations",
        indexes = {
                @Index(name = "idx_es_profile_spec", columnList = "expert_profile_id, specialization_id", unique = true)
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_es_profile_spec", columnNames = {"expert_profile_id", "specialization_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpertSpecialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_es_expert_profile"))
    private ExpertProfile expertProfile;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "specialization_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_es_specialization"))
    private Specialization specialization;

    public ExpertSpecialization(ExpertProfile expertProfile, Specialization specialization) {
        this.expertProfile = expertProfile;
        this.specialization = specialization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpertSpecialization that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
