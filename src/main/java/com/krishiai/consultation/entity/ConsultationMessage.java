package com.krishiai.consultation.entity;

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
        name = "consultation_messages",
        indexes = {
                @Index(name = "idx_cm_consultation", columnList = "consultation_id"),
                @Index(name = "idx_cm_sender", columnList = "sender_id"),
                @Index(name = "idx_cm_sent_at", columnList = "sent_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConsultationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cm_consultation"))
    private Consultation consultation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cm_sender"))
    private User sender;

    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    public ConsultationMessage(Consultation consultation, User sender, String message) {
        this.consultation = consultation;
        this.sender = sender;
        this.message = message;
        this.sentAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsultationMessage that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
