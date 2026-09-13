package com.krishiai.auth.controller;

import com.krishiai.auth.dto.ChangePasswordRequest;
import com.krishiai.auth.dto.LoginRequest;
import com.krishiai.auth.dto.RefreshTokenRequest;
import com.krishiai.auth.dto.RegisterRequest;
import com.krishiai.auth.dto.TokenResponse;
import com.krishiai.auth.service.AuthService;
import com.krishiai.common.response.ApiResponse;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new farmer or expert account.
     * Public — no authentication required.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    /**
     * Authenticate with email and password.
     * Returns both a short-lived access token and a refresh token.
     * Public — no authentication required.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        TokenResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    /**
     * Obtain a new access token using a valid refresh token.
     * Implements token rotation — the old refresh token is revoked and a new one is issued.
     * Public — authenticated via the refresh token in the body, not a Bearer header.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Log the authenticated user out by revoking their server-side refresh token.
     * The access token expires naturally (stateless design — no server-side blacklist).
     * Requires authentication: {@code Authorization: Bearer <accessToken>}.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails principal) {
        authService.logout(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * Change the authenticated user's password.
     * All existing refresh tokens are revoked on success.
     * Requires authentication: {@code Authorization: Bearer <accessToken>}.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    /**
     * Request a password reset instructions. Always returns generic message to prevent account enumeration.
     * Public — no authentication required.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody com.krishiai.auth.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If an account exists, password reset instructions have been sent."));
    }

    /**
     * Reset password using a valid reset token.
     * Public — no authentication required.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody com.krishiai.auth.dto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. Please log in with your new password."));
    }
}
