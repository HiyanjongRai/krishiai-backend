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

    @Override
    @Transactional(readOnly = true)
    public com.krishiai.common.response.PageResponse<com.krishiai.user.dto.UserResponse> getAllUsers(
            String search,
            UserRole role,
            UserStatus status,
            org.springframework.data.domain.Pageable pageable
    ) {
        org.springframework.data.jpa.domain.Specification<User> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.strip().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate namePred = cb.like(cb.lower(root.get("fullName")), pattern);
                jakarta.persistence.criteria.Predicate emailPred = cb.like(cb.lower(root.get("email")), pattern);
                jakarta.persistence.criteria.Predicate phonePred = cb.like(cb.lower(root.get("phone")), pattern);
                predicates.add(cb.or(namePred, emailPred, phonePred));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        org.springframework.data.domain.Page<User> page = userRepository.findAll(spec, pageable);
        return com.krishiai.common.response.PageResponse.from(page.map(com.krishiai.user.dto.UserResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public com.krishiai.user.dto.UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return com.krishiai.user.dto.UserResponse.from(user);
    }

    @Override
    @Transactional
    public com.krishiai.user.dto.UserResponse updateUserRole(
            Long targetUserId,
            com.krishiai.admin.dto.UpdateUserRoleRequest request,
            Long currentAdminId
    ) {
        log.info("Admin {} modifying role of user {} to {}", currentAdminId, targetUserId, request.role());

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        if (currentAdminId != null && targetUser.getId().equals(currentAdminId)) {
            throw new BadRequestException("Admins cannot modify their own role");
        }

        targetUser.setRole(request.role());
        User updated = userRepository.save(targetUser);

        // Invalidate active refresh tokens so the user re-authenticates with new privileges
        refreshTokenService.revokeAllForUser(targetUserId);

        log.info("Successfully changed role of user {} to {}", targetUserId, request.role());
        return com.krishiai.user.dto.UserResponse.from(updated);
    }
}
