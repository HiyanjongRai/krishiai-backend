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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs; // 30 days default

    @Override
    @Transactional
    public String createRefreshToken(Long userId) {
        // Revoke any existing active tokens for this user (single active token policy)
        refreshTokenRepository.revokeAllByUserId(userId);

        String rawToken = UUID.randomUUID().toString();
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
