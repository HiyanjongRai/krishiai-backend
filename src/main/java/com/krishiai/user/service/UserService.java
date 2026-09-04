package com.krishiai.user.service;

import com.krishiai.user.dto.UserResponse;
import com.krishiai.user.dto.UserUpdateRequest;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UserUpdateRequest request);

    User getUserEntity(Long userId);

    Page<UserResponse> getAllUsers(Pageable pageable);

    Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable);

    UserResponse updateUserStatus(Long userId, UserStatus status);
}
