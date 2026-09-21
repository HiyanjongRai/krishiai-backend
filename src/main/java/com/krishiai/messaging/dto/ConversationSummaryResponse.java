package com.krishiai.messaging.dto;

import com.krishiai.messaging.entity.Conversation;
import com.krishiai.messaging.entity.ConversationType;
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
public class ConversationSummaryResponse {
    private Long id;
    private ConversationType type;
    private Long consultationId;
    private String title;
    private UserPublicSummaryDto otherParticipant;
    private MessageResponse lastMessage;
    private long unreadCount;
    private LocalDateTime updatedAt;

    public static ConversationSummaryResponse fromConversation(
            Conversation conversation,
            UserPublicSummaryDto otherParticipant,
            MessageResponse lastMessage,
            long unreadCount) {
        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .consultationId(conversation.getConsultationId())
                .title(conversation.getTitle())
                .otherParticipant(otherParticipant)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
