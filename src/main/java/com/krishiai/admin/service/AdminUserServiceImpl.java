package com.krishiai.admin.service;

import com.krishiai.admin.dto.UpdateUserStatusRequest;
import com.krishiai.admin.dto.UserStatusResponse;
import com.krishiai.auth.service.RefreshTokenService;
import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserStatusResponse updateUserStatus(Long targetUserId, UpdateUserStatusRequest request, Long currentAdminId) {
        log.info("Admin {} attempting to update status of user {} to {}", currentAdminId, targetUserId, request.status());

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        if (currentAdminId != null && targetUser.getId().equals(currentAdminId)) {
            throw new BadRequestException("Admins cannot modify their own account status");
        }

        if (targetUser.getRole() == UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Cannot modify status of an admin user");
        }

        targetUser.setStatus(request.status());
        userRepository.save(targetUser);

        // If user is being blocked or deactivated, revoke all active refresh tokens immediately
        if (request.status() == UserStatus.BLOCKED ||
            request.status() == UserStatus.SUSPENDED ||
            request.status() == UserStatus.INACTIVE) {
            refreshTokenService.revokeAllForUser(targetUserId);
            log.info("Revoked all refresh tokens for user {} due to status change to {}", targetUserId, request.status());
        }

        log.info("Successfully updated status of user {} to {}", targetUserId, request.status());
        return UserStatusResponse.from(targetUser);
    }
}
