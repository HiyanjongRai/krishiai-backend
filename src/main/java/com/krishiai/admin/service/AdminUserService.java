package com.krishiai.admin.service;

import com.krishiai.admin.dto.UpdateUserStatusRequest;
import com.krishiai.admin.dto.UserStatusResponse;

public interface AdminUserService {

    /**
     * Updates the status of a target user (e.g. BLOCKED, ACTIVE, SUSPENDED, INACTIVE).
     *
     * @param targetUserId   the ID of the user whose status is being modified
     * @param request        the requested status change and optional reason
     * @param currentAdminId the ID of the authenticated admin executing the request
     * @return updated user status summary
     */
    UserStatusResponse updateUserStatus(Long targetUserId, UpdateUserStatusRequest request, Long currentAdminId);

    com.krishiai.common.response.PageResponse<com.krishiai.user.dto.UserResponse> getAllUsers(
            String search,
            com.krishiai.user.entity.UserRole role,
            com.krishiai.user.entity.UserStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    com.krishiai.user.dto.UserResponse getUserById(Long userId);

    com.krishiai.user.dto.UserResponse updateUserRole(
            Long targetUserId,
            com.krishiai.admin.dto.UpdateUserRoleRequest request,
            Long currentAdminId
    );
}
