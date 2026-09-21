package com.krishiai.messaging.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.messaging.dto.*;
import com.krishiai.messaging.service.ConversationService;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.entity.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
@Validated
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> getMyConversations(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<ConversationSummaryResponse> convs = conversationService.getMyConversations(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", convs));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<CursorPageResponse<MessageResponse>>> getMessages(
            @PathVariable @Positive Long conversationId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal CustomUserDetails principal) {
        CursorPageResponse<MessageResponse> page =
                conversationService.getMessages(principal.getUserId(), conversationId, before, limit);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", page));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable @Positive Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        MessageResponse msg = conversationService.sendMessage(principal.getUserId(), conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent", msg));
    }

    @PostMapping(value = "/conversations/{conversationId}/messages/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MessageResponse>> uploadAndSendMessage(
            @PathVariable @Positive Long conversationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String clientMessageId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        MessageResponse msg = conversationService.uploadAttachmentAndSend(
                principal.getUserId(), conversationId, file, clientMessageId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Image sent", msg));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable @Positive Long conversationId,
            @RequestParam @Positive Long upToMessageId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        conversationService.markConversationRead(principal.getUserId(), conversationId, upToMessageId);
        return ResponseEntity.ok(ApiResponse.success("Marked as read"));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable @Positive Long messageId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserRole role = principal.getUser().getRole();
        conversationService.deleteMessage(principal.getUserId(), messageId, role);
        return ResponseEntity.ok(ApiResponse.success("Message deleted"));
    }

    @PostMapping("/messages/{messageId}/report")
    public ResponseEntity<ApiResponse<Void>> reportMessage(
            @PathVariable @Positive Long messageId,
            @Valid @RequestBody ReportMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        conversationService.reportMessage(principal.getUserId(), messageId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message reported"));
    }

    @PostMapping("/admin/conversations/support")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ConversationSummaryResponse>> createSupportConversation(
            @RequestParam @Positive Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ConversationSummaryResponse conv =
                conversationService.createSupportConversation(principal.getUserId(), targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Support conversation created", conv));
    }
}
