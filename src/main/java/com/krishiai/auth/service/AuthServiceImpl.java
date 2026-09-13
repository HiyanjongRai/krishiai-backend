package com.krishiai.auth.service;

import com.krishiai.auth.dto.ChangePasswordRequest;
import com.krishiai.auth.dto.LoginRequest;
import com.krishiai.auth.dto.LoginResponse;
import com.krishiai.auth.dto.RefreshTokenRequest;
import com.krishiai.auth.dto.RegisterRequest;
import com.krishiai.auth.dto.TokenResponse;
import com.krishiai.auth.entity.RefreshToken;
import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.common.exception.UnauthorizedException;
import com.krishiai.expert.service.ExpertProfileService;
import com.krishiai.security.jwt.JwtTokenProvider;
import com.krishiai.user.dto.UserResponse;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;

    // @Lazy prevents a circular dependency: AuthService → ExpertProfileService → UserRepository → AuthService
    @Lazy
    @Autowired
    private ExpertProfileService expertProfileService;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = User.normaliseEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with email " + normalizedEmail + " already exists");
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            String cleanPhone = request.phone().strip();
            if (userRepository.existsByPhone(cleanPhone)) {
                throw new ConflictException("An account with phone number " + cleanPhone + " already exists");
            }
        }

        if (request.role() != null && request.role() == UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Public registration as administrator is strictly prohibited");
        }

        UserRole role = (request.role() != null) ? request.role() : UserRole.ROLE_FARMER;
        if (role != UserRole.ROLE_FARMER && role != UserRole.ROLE_EXPERT) {
            throw new BadRequestException("Invalid registration role. Only FARMER and EXPERT roles are allowed.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().strip());
        user.setPhone(request.phone() != null && !request.phone().isBlank() ? request.phone().strip() : null);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);
        log.info("Registered new user for KrishiAI: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        // Auto-create the ExpertProfile immediately so the expert can log in and find their profile
        if (savedUser.getRole() == UserRole.ROLE_EXPERT) {
            expertProfileService.ensureProfileExists(savedUser.getId());
            log.info("Auto-created ExpertProfile for new expert: {}", savedUser.getEmail());
        }

        return UserResponse.from(savedUser);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request, String clientIp) {
        String normalizedEmail = User.normaliseEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ForbiddenException("Account is temporarily locked due to repeated failed login attempts. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            if (user.getFailedLoginAttempts() >= 5) {
                user.lockUntil(LocalDateTime.now().plusMinutes(15));
                log.warn("Account locked for 15 minutes due to 5 failed login attempts: {}", user.getEmail());
            }
            userRepository.save(user);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check account status
        checkAccountStatus(user);

        user.recordSuccessfulLogin(clientIp);
        userRepository.save(user);

        log.info("User authenticated successfully: {} (Role: {})", user.getEmail(), user.getRole());

        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return TokenResponse.of(accessToken, jwtExpirationMs, refreshToken, UserResponse.from(user));
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        // Validate the presented refresh token
        RefreshToken storedToken = refreshTokenService.validateRefreshToken(request.refreshToken());
        Long userId = storedToken.getUserId();

        // Load the user and verify their account is still allowed to authenticate
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User associated with this refresh token no longer exists"));

        checkAccountStatus(user);

        // Token rotation: revoke old token, issue new pair
        refreshTokenService.revokeRefreshToken(request.refreshToken());
        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("Token refreshed for userId={} ({})", userId, user.getEmail());
        return TokenResponse.of(newAccessToken, jwtExpirationMs, newRefreshToken, UserResponse.from(user));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
        log.info("User logged out, refresh tokens revoked for userId={}", userId);
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (request.confirmPassword() != null && !request.confirmPassword().isBlank()) {
            if (!request.newPassword().equals(request.confirmPassword())) {
                throw new BadRequestException("New password and confirm password do not match");
            }
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens to force re-authentication on other devices
        refreshTokenService.revokeAllForUser(userId);

        log.info("Password changed successfully for userId={} ({})", userId, user.getEmail());
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void forgotPassword(com.krishiai.auth.dto.ForgotPasswordRequest request) {
        String normalizedEmail = User.normaliseEmail(request.email());
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (user.getStatus() != UserStatus.BLOCKED && user.getStatus() != UserStatus.SUSPENDED) {
                String rawToken = passwordResetTokenService.createResetToken(user.getId());
                log.info("Password reset token generated for user: {} (token length={})", user.getEmail(), rawToken.length());
            } else {
                log.warn("Password reset requested for non-active user: {}", user.getEmail());
            }
        });
        // Always succeed silently to prevent user enumeration attacks
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void resetPassword(com.krishiai.auth.dto.ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        com.krishiai.auth.entity.PasswordResetToken resetToken = passwordResetTokenService.validateResetToken(request.token());
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BadRequestException("User associated with reset token not found"));

        checkAccountStatus(user);

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        passwordResetTokenService.markTokenUsed(resetToken);
        refreshTokenService.revokeAllForUser(user.getId());

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Check account status and throw an appropriate exception if the account
     * is not allowed to authenticate. Called during login and token refresh.
     */
    private void checkAccountStatus(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ForbiddenException("Your account has been blocked. Please contact support.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Your account has been suspended. Please contact support.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ForbiddenException("Your account is currently inactive. Please contact support.");
        }

        // ROLE_EXPERT users may have UserStatus.PENDING while their professional verification
        // is in progress. They must be allowed to log in so they can complete their profile
        // and submit their verification application. The PENDING guard below only applies
        // to non-expert roles (e.g. ROLE_FARMER pending email verification).
        if (user.getStatus() == UserStatus.PENDING && user.getRole() != UserRole.ROLE_EXPERT) {
            throw new ForbiddenException("Your account is pending verification. Please check your email.");
        }
    }
}
