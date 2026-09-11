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
}
