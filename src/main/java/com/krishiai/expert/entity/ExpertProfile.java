package com.krishiai.expert.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Professional profile for a KrishiAI expert.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Expert registers → User(ROLE_EXPERT, PENDING) + ExpertProfile created automatically.</li>
 *   <li>Expert logs in, completes profile, selects crops/specializations/locations.</li>
 *   <li>Expert submits verification application (applicationStatus → SUBMITTED).</li>
 *   <li>Admin reviews → applicationStatus becomes APPROVED or REJECTED.</li>
 *   <li>On APPROVED: verifiedExpert = true, User.status → ACTIVE.</li>
 * </ol>
 */
@Entity
@Table(
        name = "expert_profiles",
        indexes = {
                @Index(name = "idx_expert_profile_user", columnList = "user_id", unique = true),
                @Index(name = "idx_expert_profile_verified", columnList = "verified_expert"),
                @Index(name = "idx_expert_profile_ver_status", columnList = "verification_status"),
                @Index(name = "idx_expert_profile_app_status", columnList = "application_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpertProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Owner of this profile. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_expert_profile_user"))
    private User user;

    // ─── Professional Information ──────────────────────────────────────────────

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    @Column(name = "bio", length = 500)
    private String bio;

    /** Years of professional agricultural experience. */
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    /** Highest academic qualification / degree. */
    @Size(max = 200, message = "Qualification must not exceed 200 characters")
    @Column(name = "qualification", length = 200)
    private String qualification;

    /** Institution where the qualification was obtained. */
    @Size(max = 200, message = "Institution name must not exceed 200 characters")
    @Column(name = "institution", length = 200)
    private String institution;

    /** Current employer / organization. */
    @Size(max = 200, message = "Organization name must not exceed 200 characters")
    @Column(name = "organization", length = 200)
    private String organization;

    /** Official title / designation. */
    @Size(max = 100, message = "Designation must not exceed 100 characters")
    @Column(name = "designation", length = 100)
    private String designation;

    /** LinkedIn URL or personal website. */
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    // ─── Verification ──────────────────────────────────────────────────────────

    /**
     * Whether this expert has been professionally verified by an admin.
     * Kept synchronized with verificationStatus == ExpertVerificationStatus.VERIFIED.
     */
    @Column(name = "verified_expert", nullable = false)
    private boolean verifiedExpert = false;

    /**
     * Discrete verification state: UNVERIFIED, VERIFIED, or REJECTED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private ExpertVerificationStatus verificationStatus = ExpertVerificationStatus.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 50)
    private ExpertApplicationStatus applicationStatus = ExpertApplicationStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Size(max = 1000, message = "Admin notes must not exceed 1000 characters")
    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    // ─── Relationships ──────────────────────────────────────────────────────────

    @org.hibernate.annotations.BatchSize(size = 30)
    @OneToMany(mappedBy = "expertProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<ExpertCropExpertise> cropExpertises = new java.util.LinkedHashSet<>();

    @org.hibernate.annotations.BatchSize(size = 30)
    @OneToMany(mappedBy = "expertProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<ExpertSpecialization> specializations = new java.util.LinkedHashSet<>();

    @org.hibernate.annotations.BatchSize(size = 30)
    @OneToMany(mappedBy = "expertProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<ExpertLocation> locations = new java.util.LinkedHashSet<>();

    @org.hibernate.annotations.BatchSize(size = 30)
    @OneToMany(mappedBy = "expertProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<ExpertDocument> documents = new java.util.LinkedHashSet<>();

    // ─── Factory ───────────────────────────────────────────────────────────────

    public static ExpertProfile createFor(User user) {
        ExpertProfile profile = new ExpertProfile();
        profile.user = user;
        profile.verifiedExpert = false;
        profile.verificationStatus = ExpertVerificationStatus.UNVERIFIED;
        profile.applicationStatus = ExpertApplicationStatus.DRAFT;
        return profile;
    }

    // ─── Domain Logic / State Machine Transitions ──────────────────────────────

    /**
     * Submits the application. Allowed from DRAFT, REJECTED, or ADDITIONAL_INFORMATION_REQUIRED.
     */
    public void submitApplication() {
        if (this.applicationStatus != ExpertApplicationStatus.DRAFT
                && this.applicationStatus != ExpertApplicationStatus.REJECTED
                && this.applicationStatus != ExpertApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED) {
            throw new IllegalStateException(
                    "Application can only be submitted from DRAFT, REJECTED, or ADDITIONAL_INFORMATION_REQUIRED state. Current: " + applicationStatus);
        }
        this.applicationStatus = ExpertApplicationStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * Admin begins review: moves SUBMITTED or ADDITIONAL_INFORMATION_REQUIRED to UNDER_REVIEW.
     */
    public void startReview() {
        if (this.applicationStatus != ExpertApplicationStatus.SUBMITTED
                && this.applicationStatus != ExpertApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED) {
            throw new IllegalStateException(
                    "Review can only be started for SUBMITTED or ADDITIONAL_INFORMATION_REQUIRED applications. Current: " + applicationStatus);
        }
        this.applicationStatus = ExpertApplicationStatus.UNDER_REVIEW;
    }

    /**
     * Admin requests additional documents or clarifications.
     */
    public void requestAdditionalInfo(String notes) {
        if (this.applicationStatus != ExpertApplicationStatus.UNDER_REVIEW
                && this.applicationStatus != ExpertApplicationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Additional information can only be requested for applications under review or submitted. Current: " + applicationStatus);
        }
        this.applicationStatus = ExpertApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = notes;
    }

    /**
     * Admin approves the application. Forbids direct DRAFT -> APPROVED.
     */
    public void approveApplication(String notes) {
        if (this.applicationStatus != ExpertApplicationStatus.UNDER_REVIEW
                && this.applicationStatus != ExpertApplicationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Application cannot be approved directly from " + applicationStatus + ". It must be submitted and reviewed first.");
        }
        this.applicationStatus = ExpertApplicationStatus.APPROVED;
        this.verificationStatus = ExpertVerificationStatus.VERIFIED;
        this.verifiedExpert = true;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = notes;
    }

    /**
     * Admin rejects the application.
     */
    public void rejectApplication(String notes) {
        if (this.applicationStatus != ExpertApplicationStatus.UNDER_REVIEW
                && this.applicationStatus != ExpertApplicationStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Application cannot be rejected from " + applicationStatus + " state.");
        }
        this.applicationStatus = ExpertApplicationStatus.REJECTED;
        this.verificationStatus = ExpertVerificationStatus.REJECTED;
        this.verifiedExpert = false;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = notes;
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpertProfile that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
