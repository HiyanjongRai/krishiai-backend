package com.krishiai.auth.service;

import com.krishiai.auth.dto.ChangePasswordRequest;
import com.krishiai.auth.dto.LoginRequest;
import com.krishiai.auth.dto.RefreshTokenRequest;
import com.krishiai.auth.dto.TokenResponse;
import com.krishiai.auth.entity.RefreshToken;
import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.common.exception.UnauthorizedException;
import com.krishiai.security.jwt.JwtTokenProvider;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);

        sampleUser = User.createFarmer("farmer@krishiai.com", "hashedPassword", "John", "Doe", "+9779801234567");
        sampleUser.setId(1L);
        sampleUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("login returns TokenResponse with both accessToken and refreshToken on valid credentials")
    void login_Success() {
        LoginRequest request = new LoginRequest("farmer@krishiai.com", "password123");

        when(userRepository.findByEmail("farmer@krishiai.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, UserRole.ROLE_FARMER)).thenReturn("jwt-access-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn("refresh-token-uuid");

        TokenResponse response = authService.login(request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt-access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-uuid");
        assertThat(response.user().email()).isEqualTo("farmer@krishiai.com");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("login throws ForbiddenException if user status is BLOCKED")
    void login_BlockedUser() {
        sampleUser.setStatus(UserStatus.BLOCKED);
        LoginRequest request = new LoginRequest("farmer@krishiai.com", "password123");

        when(userRepository.findByEmail("farmer@krishiai.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    @DisplayName("refreshToken validates token, checks user status, rotates tokens, and returns new TokenResponse")
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        RefreshToken storedToken = new RefreshToken("dummy-hash", 1L, LocalDateTime.now().plusDays(30));

        when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(storedToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateToken(1L, UserRole.ROLE_FARMER)).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn("new-refresh-token");

        TokenResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).revokeRefreshToken("valid-refresh-token");
        verify(refreshTokenService).createRefreshToken(1L);
    }

    @Test
    @DisplayName("refreshToken throws ForbiddenException if user is BLOCKED")
    void refreshToken_BlockedUser() {
        sampleUser.setStatus(UserStatus.BLOCKED);
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        RefreshToken storedToken = new RefreshToken("dummy-hash", 1L, LocalDateTime.now().plusDays(30));

        when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(storedToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("blocked");
    }

    @Test
    @DisplayName("logout revokes all refresh tokens for the user")
    void logout_Success() {
        authService.logout(1L);
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    @DisplayName("changePassword updates password hash and revokes all refresh tokens on valid request")
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "NewSecurePassword@123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewSecurePassword@123", "hashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewSecurePassword@123")).thenReturn("newHashedPassword");

        authService.changePassword(1L, request);

        assertThat(sampleUser.getPasswordHash()).isEqualTo("newHashedPassword");
        verify(userRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    @DisplayName("changePassword throws BadRequestException when current password is incorrect")
    void changePassword_IncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "NewSecurePassword@123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("changePassword throws BadRequestException when new password matches current password")
    void changePassword_SamePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("currentPassword", "currentPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("currentPassword", "hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password must be different");
    }

    @Test
    @DisplayName("changePassword throws ResourceNotFoundException if user does not exist")
    void changePassword_UserNotFound() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
