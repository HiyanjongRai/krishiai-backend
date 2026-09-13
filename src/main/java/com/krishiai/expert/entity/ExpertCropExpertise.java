package com.krishiai.expert.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.crop.entity.Crop;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an expert's crop or agricultural expertise claim.
 *
 * <p>Key business rules:
 * <ul>
 *   <li>An expert remains a VERIFIED professional even if individual expertise claims are SELF_DECLARED.</li>
 *   <li>Maximum 3 PRIMARY crops per expert.</li>
 *   <li>New claims start as SELF_DECLARED unless supporting evidence is attached (EVIDENCE_SUBMITTED).</li>
 *   <li>Only administrators can mark claims as VERIFIED or REJECTED.</li>
 *   <li>Crops can be null for non-crop agricultural domain expertise (e.g. Pest Management, Soil Management).</li>
 * </ul>
 */
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
@Entity
@Table(
        name = "expert_crop_expertises",
        indexes = {
                @Index(name = "idx_ece_profile_crop", columnList = "expert_profile_id, crop_id"),
                @Index(name = "idx_ece_profile_area", columnList = "expert_profile_id, expertise_area"),
                @Index(name = "idx_ece_profile_type", columnList = "expert_profile_id, expertise_type"),
                @Index(name = "idx_ece_crop_verified", columnList = "crop_id, verification_status"),
                @Index(name = "idx_ece_ver_status", columnList = "verification_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpertCropExpertise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ece_expert_profile"))
    private ExpertProfile expertProfile;

    /**
     * Associated crop if this claim is crop-specific.
     * Nullable for broader domain expertise (e.g. Pest Management).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "crop_id",
            foreignKey = @ForeignKey(name = "fk_ece_crop"))
    private Crop crop;

    /**
     * Agricultural domain/category (e.g. "Crop Production", "Pest Management", "Soil Management", "Irrigation").
     */
    @Column(name = "expertise_area", length = 120)
    private String expertiseArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "expertise_type", nullable = false, length = 20)
    private CropExpertiseType expertiseType = CropExpertiseType.SECONDARY;

    @Enumerated(EnumType.STRING)
    @Column(name = "expertise_level", length = 30)
    private ExpertiseLevel expertiseLevel = ExpertiseLevel.INTERMEDIATE;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private ExpertiseSourceType sourceType = ExpertiseSourceType.SELF_DECLARED;

    /**
     * Optional verification document/certificate supporting this expertise claim.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_document_id",
            foreignKey = @ForeignKey(name = "fk_ece_evidence_document"))
    private ExpertDocument evidenceDocument;

    /**
     * Individual expertise verification status.
     * Starts as SELF_DECLARED on selection, or EVIDENCE_SUBMITTED if evidence is attached.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private CropExpertiseVerificationStatus verificationStatus = CropExpertiseVerificationStatus.SELF_DECLARED;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", length = 40)
    private ExpertiseVerificationMethod verificationMethod = ExpertiseVerificationMethod.NONE;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public ExpertCropExpertise(ExpertProfile expertProfile, Crop crop, CropExpertiseType expertiseType) {
        this.expertProfile = expertProfile;
        this.crop = crop;
        this.expertiseType = expertiseType != null ? expertiseType : CropExpertiseType.SECONDARY;
        this.verificationStatus = CropExpertiseVerificationStatus.SELF_DECLARED;
        this.sourceType = ExpertiseSourceType.SELF_DECLARED;
        this.verificationMethod = ExpertiseVerificationMethod.NONE;
        this.expertiseLevel = ExpertiseLevel.INTERMEDIATE;
    }

    public ExpertCropExpertise(ExpertProfile expertProfile, Crop crop, String expertiseArea, CropExpertiseType expertiseType) {
        this(expertProfile, crop, expertiseType);
        this.expertiseArea = expertiseArea;
    }

    public void submitEvidence(ExpertDocument document, ExpertiseSourceType source) {
        this.evidenceDocument = document;
        if (source != null) {
            this.sourceType = source;
        } else if (this.sourceType == null || this.sourceType == ExpertiseSourceType.SELF_DECLARED) {
            this.sourceType = ExpertiseSourceType.CERTIFICATE;
        }
        this.verificationStatus = CropExpertiseVerificationStatus.EVIDENCE_SUBMITTED;
        this.rejectionReason = null;
    }

    public void verify(Long adminUserId, ExpertiseVerificationMethod method) {
        this.verificationStatus = CropExpertiseVerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = adminUserId;
        this.verificationMethod = method != null ? method : ExpertiseVerificationMethod.ADMIN_REVIEW;
        this.rejectionReason = null;
    }

    public void verify(Long adminUserId) {
        verify(adminUserId, ExpertiseVerificationMethod.ADMIN_REVIEW);
    }

    public void reject(Long adminUserId, String reason) {
        this.verificationStatus = CropExpertiseVerificationStatus.REJECTED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = adminUserId;
        this.rejectionReason = reason != null && !reason.isBlank()
                ? reason.trim()
                : "Supporting evidence does not sufficiently support this expertise.";
    }

    public void reject(Long adminUserId) {
        reject(adminUserId, null);
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
