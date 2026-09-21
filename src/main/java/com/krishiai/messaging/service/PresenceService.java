package com.krishiai.messaging.service;

import com.krishiai.messaging.dto.PresenceEventDto;
import com.krishiai.messaging.dto.TypingEventDto;
import com.krishiai.messaging.repository.ConversationMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ephemeral presence (online/offline) and typing indicators via WebSocket.
 * State is stored in-memory (ConcurrentHashMap) as it is transient and not persisted to DB.
 * A Redis implementation can replace this if horizontal scaling is needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationMemberRepository conversationMemberRepository;

    // Thread-safe set of online user IDs
    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    public void userConnected(Long userId, String displayName) {
        onlineUsers.add(userId);
        log.debug("User connected: userId={}", userId);
        broadcastPresence(userId, displayName, true);
    }

    public void userDisconnected(Long userId, String displayName) {
        onlineUsers.remove(userId);
        log.debug("User disconnected: userId={}", userId);
        broadcastPresence(userId, displayName, false);
    }

    public boolean isOnline(Long userId) {
        return onlineUsers.contains(userId);
    }

    public void broadcastTyping(Long conversationId, Long userId, String displayName, boolean typing) {
        TypingEventDto event = TypingEventDto.builder()
                .conversationId(conversationId)
                .userId(userId)
                .displayName(displayName)
                .typing(typing)
                .build();
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId + ".typing", event);
    }

    private void broadcastPresence(Long userId, String displayName, boolean online) {
        PresenceEventDto event = PresenceEventDto.builder()
                .userId(userId)
                .displayName(displayName)
                .online(online)
                .build();
        // Broadcast to all conversations this user is part of
        conversationMemberRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .forEach(m -> messagingTemplate.convertAndSend(
                        "/topic/conversation." + m.getConversationId() + ".presence", event));
    }
}
