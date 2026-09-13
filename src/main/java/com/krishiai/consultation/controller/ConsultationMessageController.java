package com.krishiai.consultation.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.consultation.dto.MessageResponse;
import com.krishiai.consultation.dto.SendMessageRequest;
import com.krishiai.consultation.service.ConsultationMessageService;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.entity.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farmer/consultations")
@PreAuthorize("hasAnyAuthority('ROLE_FARMER', 'ROLE_EXPERT', 'ROLE_ADMIN')")
@RequiredArgsConstructor
public class ConsultationMessageController {

    private final ConsultationMessageService messageService;

    @GetMapping("/{consultationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long consultationId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(UserRole.ROLE_ADMIN.name()));
        List<MessageResponse> messages = messageService.getMessages(consultationId, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }

    @PostMapping("/{consultationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable Long consultationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        MessageResponse response = messageService.sendMessage(consultationId, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent successfully", response));
    }
}
