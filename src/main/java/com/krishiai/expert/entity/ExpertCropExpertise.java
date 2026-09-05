package com.krishiai.expert.entity;

import com.krishiai.crop.entity.Crop;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Join table between ExpertProfile and Crop.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Maximum 3 PRIMARY crops per expert.</li>
 *   <li>Expert-submitted crops start in PENDING verification status.</li>
 *   <li>Only administrators can verify or reject individual crop expertise.</li>
 * </ul>
 */
@Entity
@Table(
        name = "expert_crop_expertises",
        indexes = {
                @Index(name = "idx_ece_profile_crop", columnList = "expert_profile_id, crop_id", unique = true),
                @Index(name = "idx_ece_profile_type", columnList = "expert_profile_id, expertise_type"),
                @Index(name = "idx_ece_crop_verified", columnList = "crop_id, verification_status"),
                @Index(name = "idx_ece_ver_status", columnList = "verification_status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ece_profile_crop", columnNames = {"expert_profile_id", "crop_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpertCropExpertise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ece_expert_profile"))
    private ExpertProfile expertProfile;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "crop_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ece_crop"))
    private Crop crop;

    @Enumerated(EnumType.STRING)
    @Column(name = "expertise_type", nullable = false, length = 20)
    private CropExpertiseType expertiseType;

    /**
     * Individual crop verification status. Starts as PENDING on selection.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private CropExpertiseVerificationStatus verificationStatus = CropExpertiseVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    public ExpertCropExpertise(ExpertProfile expertProfile, Crop crop, CropExpertiseType expertiseType) {
        this.expertProfile = expertProfile;
        this.crop = crop;
        this.expertiseType = expertiseType;
        this.verificationStatus = CropExpertiseVerificationStatus.PENDING;
    }

    public void verify(Long adminUserId) {
        this.verificationStatus = CropExpertiseVerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = adminUserId;
    }

    public void reject(Long adminUserId) {
        this.verificationStatus = CropExpertiseVerificationStatus.REJECTED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = adminUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpertCropExpertise that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
