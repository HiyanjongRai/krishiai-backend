package com.krishiai.auth.service;

import com.krishiai.auth.entity.RefreshToken;
import com.krishiai.auth.repository.RefreshTokenRepository;
import com.krishiai.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs; // 7 days default

    @Override
    @Transactional
    public String createRefreshToken(Long userId) {
        // Revoke any existing active tokens for this user (single active token policy)
        refreshTokenRepository.revokeAllByUserId(userId);

        String rawToken = generateRawToken();
        String hash = sha256(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshExpirationMs / 1000);

        RefreshToken token = new RefreshToken(hash, userId, expiresAt);
        refreshTokenRepository.save(token);

        log.debug("Created refresh token for userId={}", userId);
        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String hash = sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or unknown refresh token"));

        if (!token.isValid()) {
            if (token.isRevoked()) {
                throw new UnauthorizedException("Refresh token has been revoked");
            }
            throw new UnauthorizedException("Refresh token has expired");
        }
        return token;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String hash = sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
            log.debug("Revoked refresh token for userId={}", token.getUserId());
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId);
        log.debug("Revoked {} refresh token(s) for userId={}", count, userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String generateRawToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the Java spec — this branch is unreachable
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
