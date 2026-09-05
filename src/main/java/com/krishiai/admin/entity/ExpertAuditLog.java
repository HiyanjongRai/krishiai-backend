package com.krishiai.admin.entity;

import com.krishiai.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Immutable audit log tracking administrative verification actions on expert profiles and expertise.
 */
@Entity
@Table(
        name = "expert_audit_logs",
        indexes = {
                @Index(name = "idx_audit_target_profile", columnList = "target_profile_id"),
                @Index(name = "idx_audit_action_type", columnList = "action_type"),
                @Index(name = "idx_audit_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpertAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "performed_by_user_id", nullable = false)
    private Long performedByUserId;

    @Column(name = "performed_by_email", length = 150)
    private String performedByEmail;

    @Column(name = "target_profile_id", nullable = false)
    private Long targetProfileId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "target_crop_id")
    private Long targetCropId;

    @Column(name = "notes", length = 1000)
    private String notes;

    public ExpertAuditLog(Long performedByUserId, String performedByEmail, Long targetProfileId,
                          String actionType, String previousStatus, String newStatus,
                          Long targetCropId, String notes) {
        this.performedByUserId = performedByUserId;
        this.performedByEmail = performedByEmail;
        this.targetProfileId = targetProfileId;
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.targetCropId = targetCropId;
        this.notes = notes;
    }
}
