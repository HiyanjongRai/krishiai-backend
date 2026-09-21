package com.krishiai.messaging.entity;

import com.krishiai.common.audit.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(
        name = "conversations",
        indexes = {
                @Index(name = "idx_conversations_type", columnList = "type"),
                @Index(name = "idx_conversations_consultation", columnList = "consultation_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_conversations_consultation", columnNames = "consultation_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ConversationType type = ConversationType.CONSULTATION;

    @Column(name = "consultation_id", unique = true)
    private Long consultationId;

    @Column(name = "title", length = 200)
    private String title;

    public Conversation(ConversationType type, Long consultationId, String title) {
        this.type = type;
        this.consultationId = consultationId;
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
