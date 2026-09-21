package com.krishiai.messaging.security;

import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.consultation.entity.Consultation;
import com.krishiai.consultation.repository.ConsultationRepository;
import com.krishiai.messaging.entity.Conversation;
import com.krishiai.messaging.entity.Message;
import com.krishiai.messaging.repository.ConversationMemberRepository;
import com.krishiai.messaging.repository.ConversationRepository;
import com.krishiai.messaging.repository.MessageRepository;
import com.krishiai.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized authorization service for all messaging and consultation operations.
 * Never trusts client-supplied identity - always validates against server-side state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingAuthorizationService {

    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConsultationRepository consultationRepository;

    /**
     * Check if user can view or send messages in a conversation.
     * Must be a direct member of the conversation.
     */
    @Transactional(readOnly = true)
    public boolean canAccessConversation(Long userId, Long conversationId) {
        return conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    /**
     * Enforce conversation access; throw 403 if not a member.
     */
    @Transactional(readOnly = true)
    public void assertCanAccessConversation(Long userId, Long conversationId) {
        if (!canAccessConversation(userId, conversationId)) {
            log.warn("Access denied: userId={} attempted to access conversationId={}", userId, conversationId);
            throw new ForbiddenException("You do not have access to this conversation.");
        }
    }

    /**
     * Check if user can send a message.
     * Must be conversation member, and if consultation-based, consultation must be ACTIVE and NOT expired.
     */
    @Transactional(readOnly = true)
    public boolean canSendMessage(Long userId, Long conversationId) {
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            return false;
        }
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null && conversation.getConsultationId() != null) {
            Consultation consultation = consultationRepository.findById(conversation.getConsultationId()).orElse(null);
            if (consultation == null) {
                return false;
            }
            if (consultation.getStatus() != com.krishiai.consultation.entity.ConsultationStatus.ACTIVE || consultation.isExpired()) {
                log.info("Message sending blocked: consultation {} is in status {} (isExpired={})",
                        consultation.getId(), consultation.getStatus(), consultation.isExpired());
                return false;
            }
        }
        return true;
    }

    /**
     * Enforce send permission; throw 403 if not allowed or if consultation is expired/inactive.
     */
    @Transactional(readOnly = true)
    public void assertCanSendMessage(Long userId, Long conversationId) {
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            log.warn("Access denied: userId={} attempted to send to conversationId={}", userId, conversationId);
            throw new ForbiddenException("You do not have access to this conversation.");
        }
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null && conversation.getConsultationId() != null) {
            Consultation consultation = consultationRepository.findById(conversation.getConsultationId()).orElse(null);
            if (consultation != null && (consultation.getStatus() != com.krishiai.consultation.entity.ConsultationStatus.ACTIVE || consultation.isExpired())) {
                log.warn("Send denied: consultation {} is {} (expired={})",
                        consultation.getId(), consultation.getStatus(), consultation.isExpired());
                throw new ForbiddenException("This consultation has ended or is not active. Messages cannot be sent.");
            }
        }
    }

    /**
     * Check if user can delete a message.
     * Only the sender can delete their own messages (admins can delete any).
     */
    @Transactional(readOnly = true)
    public boolean canDeleteMessage(Long userId, Long messageId, UserRole role) {
        if (role == UserRole.ROLE_ADMIN) return true;
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) return false;
        return message.getSender().getId().equals(userId);
    }

    /**
     * Enforce delete permission; throw 403 if not allowed.
     */
    @Transactional(readOnly = true)
    public void assertCanDeleteMessage(Long userId, Long messageId, UserRole role) {
        if (!canDeleteMessage(userId, messageId, role)) {
            log.warn("Unauthorized delete attempt: userId={} on messageId={}", userId, messageId);
            throw new ForbiddenException("You are not allowed to delete this message.");
        }
    }

    /**
     * Check if user can access a consultation.
     * Farmer must own it, Expert must be assigned, Admin always has access.
     */
    @Transactional(readOnly = true)
    public boolean canAccessConsultation(Long userId, Long consultationId, UserRole role) {
        if (role == UserRole.ROLE_ADMIN) return true;
        Consultation c = consultationRepository.findById(consultationId).orElse(null);
        if (c == null) return false;
        boolean isFarmer = c.getFarmer() != null && c.getFarmer().getId().equals(userId);
        boolean isExpert = c.getExpert() != null && c.getExpert().getId().equals(userId);
        return isFarmer || isExpert;
    }

    /**
     * Enforce consultation access; throw 403 or 404 if not allowed.
     */
    @Transactional(readOnly = true)
    public Consultation assertCanAccessConsultation(Long userId, Long consultationId, UserRole role) {
        Consultation c = consultationRepository.findByIdWithDetails(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with id: " + consultationId));

        if (role != UserRole.ROLE_ADMIN) {
            boolean isFarmer = c.getFarmer() != null && c.getFarmer().getId().equals(userId);
            boolean isExpert = c.getExpert() != null && c.getExpert().getId().equals(userId);
            if (!isFarmer && !isExpert) {
                log.warn("Access denied: userId={} attempted to access consultationId={}", userId, consultationId);
                throw new ForbiddenException("You do not have access to this consultation.");
            }
        }
        return c;
    }

    /**
     * Check if user is the farmer of a consultation (for farmer-specific actions).
     */
    @Transactional(readOnly = true)
    public boolean isConsultationFarmer(Long userId, Long consultationId) {
        Consultation c = consultationRepository.findById(consultationId).orElse(null);
        return c != null && c.getFarmer() != null && c.getFarmer().getId().equals(userId);
    }

    /**
     * Check if user is the assigned expert of a consultation.
     */
    @Transactional(readOnly = true)
    public boolean isConsultationExpert(Long userId, Long consultationId) {
        Consultation c = consultationRepository.findById(consultationId).orElse(null);
        return c != null && c.getExpert() != null && c.getExpert().getId().equals(userId);
    }
}
