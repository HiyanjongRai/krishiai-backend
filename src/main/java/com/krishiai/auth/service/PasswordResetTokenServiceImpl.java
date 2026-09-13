package com.krishiai.auth.service;

import com.krishiai.auth.entity.PasswordResetToken;
import com.krishiai.auth.repository.PasswordResetTokenRepository;
import com.krishiai.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository resetTokenRepository;

    @Value("${app.auth.password-reset-expiration-minutes:15}")
    private long resetExpirationMinutes;

    @Override
    @Transactional
    public String createResetToken(Long userId) {
        // Invalidate any existing unused reset tokens for this user
        resetTokenRepository.invalidateAllByUserId(userId);

        String rawToken = generateRawToken();
        String hash = sha256(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resetExpirationMinutes);
        PasswordResetToken token = new PasswordResetToken(hash, userId, expiresAt);
        resetTokenRepository.save(token);

        log.debug("Created password reset token for userId={}", userId);
        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordResetToken validateResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Password reset token is required");
        }

        String hash = sha256(rawToken);
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (!token.isValid()) {
            if (token.isUsed()) {
                throw new BadRequestException("Password reset token has already been used");
            }
            throw new BadRequestException("Password reset token has expired");
        }

        return token;
    }

    @Override
    @Transactional
    public void markTokenUsed(PasswordResetToken token) {
        token.markUsed();
        resetTokenRepository.save(token);
        log.debug("Marked password reset token as used for userId={}", token.getUserId());
    }

    @Override
    @Transactional
    public void invalidateAllForUser(Long userId) {
        resetTokenRepository.invalidateAllByUserId(userId);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
