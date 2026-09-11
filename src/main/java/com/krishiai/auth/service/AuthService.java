package com.krishiai.auth.service;

import com.krishiai.auth.dto.ChangePasswordRequest;
import com.krishiai.auth.dto.LoginRequest;
import com.krishiai.auth.dto.LoginResponse;
import com.krishiai.auth.dto.RefreshTokenRequest;
import com.krishiai.auth.dto.RegisterRequest;
import com.krishiai.auth.dto.TokenResponse;
import com.krishiai.user.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    /**
     * Authenticate with email/password. Returns both an access token and a refresh token.
     * The login response is now a {@link TokenResponse} (backward-compatible superset of
     * the previous {@link LoginResponse}).
     */
    TokenResponse login(LoginRequest request, String clientIp);

    /**
     * Refresh an access token using a valid server-side refresh token.
     * Implements token rotation: the supplied token is revoked and a new one is issued.
     */
    TokenResponse refreshToken(RefreshTokenRequest request);

    /**
     * Revoke the caller's server-side refresh token.
     * The access token remains valid until its natural expiry (stateless design).
     *
     * @param userId the authenticated user whose refresh token(s) should be revoked
     */
    void logout(Long userId);

    /**
     * Change the authenticated user's password.
     * All existing refresh tokens for the user are revoked on success.
     *
     * @param userId  the authenticated user (never taken from request body)
     * @param request current + new password
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
