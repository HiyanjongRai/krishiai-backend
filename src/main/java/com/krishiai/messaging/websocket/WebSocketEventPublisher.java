package com.krishiai.messaging.websocket;

import com.krishiai.consultation.entity.ConsultationStatus;
import com.krishiai.messaging.dto.MessageResponse;
import com.krishiai.messaging.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes real-time events to WebSocket topics.
 * Centralized so broadcast logic doesn't leak into services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a new message to all subscribers of the conversation topic.
     */
    public void broadcastMessage(Message message, Long conversationId, boolean senderOnline, String deliveryStatus) {
        MessageResponse response = MessageResponse.fromEntity(message, senderOnline, deliveryStatus);
        String destination = "/topic/conversation." + conversationId;
        messagingTemplate.convertAndSend(destination, response);
        log.debug("Broadcast message id={} to {}", message.getId(), destination);
    }

    /**
     * Broadcast a consultation status change to relevant participants.
     */
    public void broadcastConsultationUpdate(Long consultationId, ConsultationStatus status,
                                             Long conversationId, List<Long> userIds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("consultationId", consultationId);
        payload.put("status", status.name());
        payload.put("conversationId", conversationId);

        for (Long userId : userIds) {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", payload);
        }
        log.debug("Broadcast consultation update consultationId={} status={}", consultationId, status);
    }

    /**
     * Broadcast a read receipt update to the conversation topic.
     */
    public void broadcastReadReceipt(Long conversationId, Long userId, Long lastReadMessageId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("userId", userId);
        payload.put("lastReadMessageId", lastReadMessageId);
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId + ".read", (Object) payload);
    }

    /**
     * Broadcast a delivery receipt when a message arrives.
     */
    public void broadcastDeliveryReceipt(Long conversationId, Long messageId, Long senderId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("messageId", messageId);
        payload.put("status", "DELIVERED");
        messagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/delivery", payload);
    }
}
