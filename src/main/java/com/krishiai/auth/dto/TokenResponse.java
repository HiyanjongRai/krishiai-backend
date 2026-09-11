package com.krishiai.auth.dto;

import com.krishiai.user.dto.UserResponse;

/**
 * Full authentication response returned after a successful login or token refresh.
 * Extends the original {@link LoginResponse} contract to include the refresh token.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String refreshToken,
        UserResponse user
) {
    public static TokenResponse of(String accessToken, long expiresInMs,
                                   String refreshToken, UserResponse user) {
        return new TokenResponse(accessToken, "Bearer", expiresInMs, refreshToken, user);
    }
}
