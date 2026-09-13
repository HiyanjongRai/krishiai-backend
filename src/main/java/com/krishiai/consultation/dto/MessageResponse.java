package com.krishiai.consultation.dto;

import com.krishiai.consultation.entity.ConsultationMessage;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long consultationId,
        Long senderId,
        String senderName,
        String senderRole,
        String message,
        LocalDateTime sentAt
) {
    public static MessageResponse from(ConsultationMessage msg) {
        return new MessageResponse(
                msg.getId(),
                msg.getConsultation().getId(),
                msg.getSender().getId(),
                msg.getSender().getFullName().strip(),
                msg.getSender().getRole().name(),
                msg.getMessage(),
                msg.getSentAt()
        );
    }
}
