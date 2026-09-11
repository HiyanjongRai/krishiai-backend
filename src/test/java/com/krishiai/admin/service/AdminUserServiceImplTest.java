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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User targetUser;

    @BeforeEach
    void setUp() {
        targetUser = User.createFarmer("farmer@krishiai.com", "hash", "John", "Doe", "+9779801234567");
        targetUser.setId(2L);
        targetUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("updateUserStatus to BLOCKED updates user, saves, and revokes all refresh tokens")
    void updateUserStatus_BlockUser_Success() {
        Long adminId = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BLOCKED, "Violation of terms");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserStatusResponse response = adminUserService.updateUserStatus(2L, request, adminId);

        assertThat(response.status()).isEqualTo(UserStatus.BLOCKED);
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(userRepository).save(targetUser);
        verify(refreshTokenService).revokeAllForUser(2L);
    }

    @Test
    @DisplayName("updateUserStatus to ACTIVE unblocks user without revoking tokens")
    void updateUserStatus_UnblockUser_Success() {
        targetUser.setStatus(UserStatus.BLOCKED);
        Long adminId = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.ACTIVE, "Appeal approved");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserStatusResponse response = adminUserService.updateUserStatus(2L, request, adminId);

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(targetUser);
        verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    }

    @Test
    @DisplayName("updateUserStatus throws BadRequestException when admin attempts to modify their own status")
    void updateUserStatus_AdminModifyingSelf() {
        Long adminId = 2L; // Target is also 2L
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BLOCKED, "Self block");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        assertThatThrownBy(() -> adminUserService.updateUserStatus(2L, request, adminId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot modify their own account status");
    }

    @Test
    @DisplayName("updateUserStatus throws BadRequestException when admin attempts to modify another admin's status")
    void updateUserStatus_AdminModifyingAnotherAdmin() {
        User anotherAdmin = new User();
        anotherAdmin.setId(3L);
        anotherAdmin.setRole(UserRole.ROLE_ADMIN);

        Long adminId = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BLOCKED, "Block admin");

        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherAdmin));

        assertThatThrownBy(() -> adminUserService.updateUserStatus(3L, request, adminId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot modify status of an admin user");
    }

    @Test
    @DisplayName("updateUserStatus throws ResourceNotFoundException when target user does not exist")
    void updateUserStatus_UserNotFound() {
        Long adminId = 1L;
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BLOCKED, "reason");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.updateUserStatus(999L, request, adminId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }
}
