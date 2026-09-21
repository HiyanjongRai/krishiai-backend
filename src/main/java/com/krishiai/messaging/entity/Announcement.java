package com.krishiai.messaging.entity;

import com.krishiai.common.audit.BaseEntity;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
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
        name = "announcements",
        indexes = {
                @Index(name = "idx_announcements_target_role", columnList = "target_role"),
                @Index(name = "idx_announcements_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @NotBlank
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_announcements_creator"))
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", length = 30)
    private UserRole targetRole;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Announcement(String title, String content, User createdBy, UserRole targetRole, LocalDateTime expiresAt) {
        this.title = title;
        this.content = content;
        this.createdBy = createdBy;
        this.targetRole = targetRole;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Announcement that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
