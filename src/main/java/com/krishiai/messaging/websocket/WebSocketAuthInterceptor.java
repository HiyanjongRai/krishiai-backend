package com.krishiai.messaging.websocket;

import com.krishiai.security.userdetails.CustomUserDetailsService;
import com.krishiai.security.jwt.JwtTokenProvider;
import com.krishiai.messaging.repository.ConversationMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WebSocket Channel Interceptor for JWT-based authentication and per-subscription authorization.
 *
 * Security contract:
 * - CONNECT: Validates JWT from STOMP header, sets authenticated principal.
 * - SUBSCRIBE: Verifies the user is a member of the conversation they are subscribing to.
 * - SEND: Verifies the user is a member of the target conversation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ConversationMemberRepository conversationMemberRepository;
    private final com.krishiai.messaging.security.MessagingAuthorizationService authorizationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            case SEND -> handleSend(accessor);
            default -> { /* allow other frames */ }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket CONNECT rejected: invalid or missing JWT");
            throw new org.springframework.security.access.AccessDeniedException("Invalid authentication token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserById(userId);

        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            log.warn("WebSocket CONNECT rejected: userId={} account disabled or locked", userId);
            throw new org.springframework.security.access.AccessDeniedException("Account is not active");
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(auth);
        log.debug("WebSocket CONNECT authorized: userId={}", userId);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        Long userId = extractAuthenticatedUserId(accessor);
        if (userId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }

        // Enforce membership check for conversation subscriptions
        Long conversationId = extractConversationId(destination);
        if (conversationId != null) {
            if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
                log.warn("WebSocket SUBSCRIBE denied: userId={} not member of conversationId={}", userId, conversationId);
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not a member of conversation " + conversationId);
            }
            log.debug("WebSocket SUBSCRIBE authorized: userId={}, conversationId={}", userId, conversationId);
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        Long userId = extractAuthenticatedUserId(accessor);
        if (userId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }

        // For /app/conversation.{id}.typing or similar, verify membership & active status
        Long conversationId = extractConversationIdFromApp(destination);
        if (conversationId != null) {
            if (!authorizationService.canSendMessage(userId, conversationId)) {
                log.warn("WebSocket SEND denied: userId={} cannot send to conversationId={}", userId, conversationId);
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not allowed to send messages in conversation " + conversationId + " (consultation expired or inactive)");
            }
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        // Also check the passcode field used by some STOMP clients
        String passcode = accessor.getPasscode();
        if (StringUtils.hasText(passcode) && passcode.startsWith("Bearer ")) {
            return passcode.substring(7);
        }
        return null;
    }

    private Long extractAuthenticatedUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authToken &&
                authToken.getPrincipal() instanceof com.krishiai.security.userdetails.CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    private Long extractConversationId(String destination) {
        // Pattern: /topic/conversation.{id} or /queue/conversation.{id}
        if (destination == null) return null;
        try {
            if (destination.matches("/topic/conversation\\.\\d+.*") || destination.matches("/queue/conversation\\.\\d+.*")) {
                String[] parts = destination.split("\\.");
                if (parts.length >= 2) {
                    return Long.parseLong(parts[1].split("/")[0]);
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse conversation ID from destination: {}", destination);
        }
        return null;
    }

    private Long extractConversationIdFromApp(String destination) {
        // Pattern: /app/conversation.{id}.typing
        if (destination == null) return null;
        try {
            if (destination.startsWith("/app/conversation.")) {
                String[] parts = destination.split("\\.");
                if (parts.length >= 2) {
                    return Long.parseLong(parts[1].split("/")[0]);
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse conversation ID from app destination: {}", destination);
        }
        return null;
    }
}
