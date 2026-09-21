package com.krishiai.messaging.dto;

import com.krishiai.messaging.entity.Message;
import com.krishiai.messaging.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private UserPublicSummaryDto sender;
    private String content;
    private MessageType messageType;
    private String attachmentUrl;
    private String attachmentPublicId;
    private String attachmentMimeType;
    private Long attachmentFileSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    private String deliveryStatus; // SENT, DELIVERED, READ
    private String clientMessageId;

    public static MessageResponse fromEntity(Message message, boolean online, String deliveryStatus) {
        if (message == null) return null;
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .sender(UserPublicSummaryDto.fromUser(message.getSender(), online))
                .content(message.isDeleted() ? "This message was deleted." : message.getContent())
                .messageType(message.getMessageType())
                .attachmentUrl(message.isDeleted() ? null : message.getAttachmentUrl())
                .attachmentPublicId(message.isDeleted() ? null : message.getAttachmentPublicId())
                .attachmentMimeType(message.isDeleted() ? null : message.getAttachmentMimeType())
                .attachmentFileSize(message.isDeleted() ? null : message.getAttachmentFileSize())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .deleted(message.isDeleted())
                .deliveryStatus(deliveryStatus != null ? deliveryStatus : "SENT")
                .build();
    }
}
