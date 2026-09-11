package com.krishiai.admin.controller;

import com.krishiai.admin.dto.UpdateUserStatusRequest;
import com.krishiai.admin.dto.UserStatusResponse;
import com.krishiai.admin.service.AdminUserService;
import com.krishiai.common.response.ApiResponse;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.entity.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserStatusResponse>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long adminId = (principal != null) ? principal.getUserId() : null;
        UserStatusResponse response = adminUserService.updateUserStatus(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(
                "User status updated to " + request.status(),
                response
        ));
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<UserStatusResponse>> blockUser(
            @PathVariable Long userId,
            @RequestBody(required = false) UpdateUserStatusRequest optionalRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long adminId = (principal != null) ? principal.getUserId() : null;
        String reason = (optionalRequest != null) ? optionalRequest.reason() : "Blocked by admin";
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BLOCKED, reason);
        UserStatusResponse response = adminUserService.updateUserStatus(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(
                "User blocked successfully",
                response
        ));
    }

    @PostMapping("/{userId}/unblock")
    public ResponseEntity<ApiResponse<UserStatusResponse>> unblockUser(
            @PathVariable Long userId,
            @RequestBody(required = false) UpdateUserStatusRequest optionalRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long adminId = (principal != null) ? principal.getUserId() : null;
        String reason = (optionalRequest != null) ? optionalRequest.reason() : "Unblocked by admin";
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.ACTIVE, reason);
        UserStatusResponse response = adminUserService.updateUserStatus(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(
                "User unblocked successfully",
                response
        ));
    }
}
