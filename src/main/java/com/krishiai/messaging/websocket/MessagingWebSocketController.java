package com.krishiai.messaging.websocket;

import com.krishiai.messaging.dto.TypingEventDto;
import com.krishiai.messaging.service.PresenceService;
import com.krishiai.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * STOMP message handler for real-time messaging events:
 * - Typing indicators
 * - Presence tracking (connect/disconnect)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MessagingWebSocketController {

    private final PresenceService presenceService;

    /**
     * Handle typing indicator events.
     * Client sends: /app/conversation.{id}.typing with {typing: true/false}
     * Auth interceptor already validated membership before this is called.
     */
    @MessageMapping("/conversation.{conversationId}.typing")
    public void handleTyping(
            @DestinationVariable Long conversationId,
            TypingEventDto typingEvent,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) return;
        presenceService.broadcastTyping(
                conversationId,
                principal.getUserId(),
                principal.getUser().getFullName(),
                typingEvent.isTyping()
        );
    }

    /**
     * Track user presence on WebSocket CONNECT.
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken authToken &&
                authToken.getPrincipal() instanceof CustomUserDetails userDetails) {
            presenceService.userConnected(userDetails.getUserId(), userDetails.getUser().getFullName());
        }
    }

    /**
     * Track user presence on WebSocket DISCONNECT.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken authToken &&
                authToken.getPrincipal() instanceof CustomUserDetails userDetails) {
            presenceService.userDisconnected(userDetails.getUserId(), userDetails.getUser().getFullName());
        }
    }
}
