package com.krishiai.expert.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Verification document uploaded by an expert for professional identity & credential verification.
 */
@Entity
@Table(
        name = "expert_documents",
        indexes = {
                @Index(name = "idx_ed_profile", columnList = "expert_profile_id"),
                @Index(name = "idx_ed_type", columnList = "expert_profile_id, document_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpertDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expert_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ed_expert_profile"))
    private ExpertProfile expertProfile;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // IDENTITY, EDUCATION, LICENSE, EXPERIENCE, OTHER

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType; // e.g. application/pdf, image/png

    @Column(name = "file_size", length = 50)
    private String fileSize; // e.g. 2.4 MB

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl; // URL or preview data URL

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExpertDocumentStatus status = ExpertDocumentStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    public ExpertDocument(ExpertProfile expertProfile, String documentType, String title,
                          String fileName, String fileType, String fileSize, String fileUrl) {
        this.expertProfile = expertProfile;
        this.documentType = documentType;
        this.title = title;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.uploadedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpertDocument that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
