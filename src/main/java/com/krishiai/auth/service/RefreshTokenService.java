package com.krishiai.auth.service;

import com.krishiai.auth.entity.RefreshToken;

/**
 * Manages the lifecycle of server-side refresh tokens for KrishiAI.
 *
 * <p>Only a SHA-256 hash of each token is stored in the database.
 * The raw token (a UUID) is returned to the caller exactly once and is never persisted.</p>
 */
public interface RefreshTokenService {

    /**
     * Generate and persist a new refresh token for the given user.
     * Any existing active tokens for the user are revoked first (single active token policy).
     *
     * @param userId the ID of the authenticated user
     * @return the raw (plaintext) refresh token — deliver to the client and do not store
     */
    String createRefreshToken(Long userId);

    /**
     * Validate the raw refresh token and return the stored record.
     * Throws {@link com.krishiai.common.exception.UnauthorizedException} if the token
     * is not found, has been revoked, or has expired.
     *
     * @param rawToken the plaintext token received from the client
     * @return the valid {@link RefreshToken} entity
     */
    RefreshToken validateRefreshToken(String rawToken);

    /**
     * Revoke a specific refresh token by its raw value.
     * Silently does nothing if the token is not found.
     *
     * @param rawToken the plaintext token to revoke
     */
    void revokeRefreshToken(String rawToken);

    /**
     * Revoke all refresh tokens belonging to a user.
     * Used on logout, password change, and admin block operations.
     *
     * @param userId the user whose tokens should be revoked
     */
    void revokeAllForUser(Long userId);
}
