package com.krishiai.messaging.entity;

import com.krishiai.common.audit.BaseEntity;
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
        name = "messages",
        indexes = {
                @Index(name = "idx_messages_conv_created", columnList = "conversation_id, created_at"),
                @Index(name = "idx_messages_conv_id", columnList = "conversation_id, id"),
                @Index(name = "idx_messages_sender", columnList = "sender_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = @ForeignKey(name = "fk_messages_sender"))
    private User sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "attachment_public_id", length = 255)
    private String attachmentPublicId;

    @Column(name = "attachment_mime_type", length = 100)
    private String attachmentMimeType;

    @Column(name = "attachment_file_size")
    private Long attachmentFileSize;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Message(Long conversationId, User sender, String content, MessageType messageType) {
        this.conversationId = conversationId;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
