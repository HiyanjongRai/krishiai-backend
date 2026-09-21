package com.krishiai.messaging.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "message_reports",
        indexes = {
                @Index(name = "idx_mr_message", columnList = "message_id"),
                @Index(name = "idx_mr_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MessageReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false, foreignKey = @ForeignKey(name = "fk_mr_reporter"))
    private User reportedBy;

    @NotBlank
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    public MessageReport(Long messageId, User reportedBy, String reason) {
        this.messageId = messageId;
        this.reportedBy = reportedBy;
        this.reason = reason;
        this.status = ReportStatus.OPEN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageReport that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
