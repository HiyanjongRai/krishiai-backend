package com.krishiai.messaging.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.media.constant.CloudinaryFolder;
import com.krishiai.media.dto.MediaResponse;
import com.krishiai.media.service.CloudinaryService;
import com.krishiai.messaging.dto.*;
import com.krishiai.messaging.entity.*;
import com.krishiai.messaging.repository.*;
import com.krishiai.messaging.security.MessagingAuthorizationService;
import com.krishiai.messaging.websocket.WebSocketEventPublisher;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int DEFAULT_PAGE_LIMIT = 30;
    private static final int MAX_PAGE_LIMIT = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageReportRepository messageReportRepository;
    private final UserRepository userRepository;
    private final MessagingAuthorizationService authorizationService;
    private final PresenceService presenceService;
    private final WebSocketEventPublisher eventPublisher;
    private final CloudinaryService cloudinaryService;

    /**
     * Get all conversations for the current user (paginated list view).
     */
    @Transactional
    public List<ConversationSummaryResponse> getMyConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId);

        return conversations.stream().map(conv -> {
            List<ConversationMember> members = conversationMemberRepository.findByConversationIdWithUser(conv.getId());

            // Find the other participant (not the current user)
            User otherUser = members.stream()
                    .filter(m -> !m.getUser().getId().equals(userId))
                    .map(ConversationMember::getUser)
                    .findFirst().orElse(null);

            UserPublicSummaryDto otherParticipant = otherUser != null
                    ? UserPublicSummaryDto.fromUser(otherUser, presenceService.isOnline(otherUser.getId()))
                    : null;

            // Get last message
            Optional<Message> lastMsg = messageRepository.findLatestMessageByConversationId(conv.getId());
            MessageResponse lastMsgDto = lastMsg.map(m ->
                    MessageResponse.fromEntity(m, presenceService.isOnline(m.getSender().getId()), "SENT")
            ).orElse(null);

            // Calculate unread count for the current user
            ConversationMember myMembership = members.stream()
                    .filter(m -> m.getUser().getId().equals(userId))
                    .findFirst().orElse(null);

            long unread = myMembership != null
                    ? messageRepository.countUnreadMessages(conv.getId(), myMembership.getLastReadMessageId(), userId)
                    : 0;

            return ConversationSummaryResponse.fromConversation(conv, otherParticipant, lastMsgDto, unread);
        }).collect(Collectors.toList());
    }

    /**
     * Get paginated messages for a conversation (cursor-based).
     */
    @Transactional
    public CursorPageResponse<MessageResponse> getMessages(Long userId, Long conversationId, Long beforeId, int limit) {
        authorizationService.assertCanAccessConversation(userId, conversationId);

        int safeLimit = Math.min(Math.max(1, limit), MAX_PAGE_LIMIT);

        List<Message> messages;
        if (beforeId != null) {
            messages = messageRepository.findByConversationIdAndBeforeId(conversationId, beforeId, PageRequest.of(0, safeLimit + 1));
        } else {
            messages = messageRepository.findLatestByConversationId(conversationId, PageRequest.of(0, safeLimit + 1));
        }

        boolean hasMore = messages.size() > safeLimit;
        if (hasMore) {
            messages = messages.subList(0, safeLimit);
        }

        // Reverse so chronological order (we fetched desc)
        Collections.reverse(messages);

        Long nextCursor = hasMore && !messages.isEmpty() ? messages.get(0).getId() : null;

        List<MessageResponse> dtos = messages.stream().map(m ->
                MessageResponse.fromEntity(m, presenceService.isOnline(m.getSender().getId()), "SENT")
        ).collect(Collectors.toList());

        return CursorPageResponse.<MessageResponse>builder()
                .items(dtos)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .limit(safeLimit)
                .build();
    }

    /**
     * Send a text or image message to a conversation.
     * Sender is always taken from the authenticated context - never from the request body.
     */
    @Transactional
    public MessageResponse sendMessage(Long senderId, Long conversationId, SendMessageRequest request) {
        authorizationService.assertCanSendMessage(senderId, conversationId);

        // Validate content
        String content = request.getContent();
        MessageType msgType = request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT;

        if (msgType == MessageType.TEXT && (content == null || content.isBlank())) {
            throw new BadRequestException("Message content cannot be empty");
        }

        if (content != null && content.length() > 5000) {
            throw new BadRequestException("Message content exceeds maximum length of 5000 characters");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Message message = new Message(conversationId, sender, content != null ? content : "", msgType);

        // Attach image metadata if present (already uploaded to Cloudinary)
        if (msgType == MessageType.IMAGE && request.getAttachmentUrl() != null) {
            message.setAttachmentUrl(request.getAttachmentUrl());
            message.setAttachmentPublicId(request.getAttachmentPublicId());
            message.setAttachmentMimeType(request.getAttachmentMimeType());
            message.setAttachmentFileSize(request.getAttachmentFileSize());
        }

        message = messageRepository.save(message);

        // Touch conversation to update its timestamp (Spring Auditing handles updatedAt)
        conversationRepository.findById(conversationId).ifPresent(conversationRepository::save);

        // Broadcast to all subscribers of this conversation
        boolean senderOnline = presenceService.isOnline(senderId);
        MessageResponse response = MessageResponse.fromEntity(message, senderOnline, "SENT");
        response.setClientMessageId(request.getClientMessageId()); // For deduplication on client
        eventPublisher.broadcastMessage(message, conversationId, senderOnline, "SENT");

        log.info("Message sent: id={}, conversationId={}, senderId={}", message.getId(), conversationId, senderId);
        return response;
    }

    /**
     * Upload an image attachment to Cloudinary and return metadata.
     * The client then sends a message with the returned URL and publicId.
     */
    @Transactional
    public MessageResponse uploadAttachmentAndSend(Long senderId, Long conversationId,
                                                    MultipartFile file, String clientMessageId) {
        authorizationService.assertCanSendMessage(senderId, conversationId);

        // Validate file type (only image types allowed)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed as message attachments");
        }

        // Max 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Attachment exceeds maximum size of 10MB");
        }

        MediaResponse media = cloudinaryService.uploadImage(file, CloudinaryFolder.MESSAGING_ATTACHMENTS, senderId);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("[Image]");
        req.setMessageType(MessageType.IMAGE);
        req.setAttachmentUrl(media.secureUrl());
        req.setAttachmentPublicId(media.publicId());
        req.setAttachmentMimeType(contentType);
        req.setAttachmentFileSize(file.getSize());
        req.setClientMessageId(clientMessageId);

        return sendMessage(senderId, conversationId, req);
    }

    /**
     * Soft-delete a message. Cleans up Cloudinary assets when applicable.
     */
    @Transactional
    public void deleteMessage(Long userId, Long messageId, UserRole role) {
        authorizationService.assertCanDeleteMessage(userId, messageId, role);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (message.isDeleted()) {
            throw new BadRequestException("Message is already deleted");
        }

        // Cleanup Cloudinary asset if present
        if (message.getAttachmentPublicId() != null && !message.getAttachmentPublicId().isBlank()) {
            try {
                boolean isAdmin = role == UserRole.ROLE_ADMIN;
                cloudinaryService.deleteMedia(message.getAttachmentPublicId(), userId, isAdmin);
            } catch (Exception e) {
                log.warn("Failed to delete Cloudinary asset for messageId={}: {}", messageId, e.getMessage());
            }
        }

        message.setDeletedAt(LocalDateTime.now());
        message.setContent("[deleted]");
        message.setAttachmentUrl(null);
        message.setAttachmentPublicId(null);
        messageRepository.save(message);

        log.info("Message soft-deleted: id={}, userId={}", messageId, userId);
    }

    /**
     * Mark all messages in a conversation as read up to the given message ID.
     */
    @Transactional
    public void markConversationRead(Long userId, Long conversationId, Long upToMessageId) {
        authorizationService.assertCanAccessConversation(userId, conversationId);

        ConversationMember member = conversationMemberRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this conversation"));

        member.setLastReadMessageId(upToMessageId);
        conversationMemberRepository.save(member);

        eventPublisher.broadcastReadReceipt(conversationId, userId, upToMessageId);
        log.debug("Marked read: userId={}, conversationId={}, upToMessageId={}", userId, conversationId, upToMessageId);
    }

    /**
     * Report a message for moderation.
     */
    @Transactional
    public void reportMessage(Long reporterId, Long messageId, ReportMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Verify reporter has access to this message's conversation
        authorizationService.assertCanAccessConversation(reporterId, message.getConversationId());

        // Prevent duplicate reports from the same user
        if (messageReportRepository.existsByMessageIdAndReportedById(messageId, reporterId)) {
            throw new ConflictException("You have already reported this message");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MessageReport report = new MessageReport(messageId, reporter, request.getReason());
        messageReportRepository.save(report);

        log.info("Message reported: messageId={}, reporterId={}", messageId, reporterId);
    }

    /**
     * Create an admin-to-user support conversation.
     */
    @Transactional
    public ConversationSummaryResponse createSupportConversation(Long adminId, Long targetUserId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Target user is not active");
        }

        // Check if support conversation already exists between these two users
        String titleKey = "SUPPORT-" + Math.min(adminId, targetUserId) + "-" + Math.max(adminId, targetUserId);
        Optional<Conversation> existing = conversationRepository.findByTypeAndTitle(ConversationType.ADMIN_SUPPORT, titleKey);
        if (existing.isPresent()) {
            return buildSummaryForConversation(existing.get(), adminId);
        }

        Conversation conversation = new Conversation(ConversationType.ADMIN_SUPPORT, null, titleKey);
        conversation = conversationRepository.save(conversation);

        conversationMemberRepository.save(new ConversationMember(conversation.getId(), admin));
        conversationMemberRepository.save(new ConversationMember(conversation.getId(), targetUser));

        // Welcome system message
        Message welcomeMsg = new Message(conversation.getId(), admin,
                "KrishiAI Support has initiated a conversation with you.", MessageType.SYSTEM);
        messageRepository.save(welcomeMsg);

        log.info("Support conversation created: id={}, adminId={}, targetUserId={}", conversation.getId(), adminId, targetUserId);
        return buildSummaryForConversation(conversation, adminId);
    }

    private ConversationSummaryResponse buildSummaryForConversation(Conversation conv, Long currentUserId) {
        List<ConversationMember> members = conversationMemberRepository.findByConversationIdWithUser(conv.getId());
        User otherUser = members.stream()
                .filter(m -> !m.getUser().getId().equals(currentUserId))
                .map(ConversationMember::getUser)
                .findFirst().orElse(null);
        UserPublicSummaryDto otherParticipant = otherUser != null
                ? UserPublicSummaryDto.fromUser(otherUser, presenceService.isOnline(otherUser.getId()))
                : null;
        return ConversationSummaryResponse.fromConversation(conv, otherParticipant, null, 0);
    }
}
