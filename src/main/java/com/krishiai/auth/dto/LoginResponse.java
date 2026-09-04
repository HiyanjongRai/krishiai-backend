package com.krishiai.auth.dto;

import com.krishiai.user.dto.UserResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
    public static LoginResponse of(String token, long expiresInMs, UserResponse user) {
        return new LoginResponse(token, "Bearer", expiresInMs, user);
    }
}
