package com.krishiai.messaging.entity;

import com.krishiai.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "conversation_members",
        indexes = {
                @Index(name = "idx_cm_conversation", columnList = "conversation_id"),
                @Index(name = "idx_cm_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_conversation_member", columnNames = {"conversation_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cm_user"))
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    public ConversationMember(Long conversationId, User user) {
        this.conversationId = conversationId;
        this.user = user;
        this.joinedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationMember that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
