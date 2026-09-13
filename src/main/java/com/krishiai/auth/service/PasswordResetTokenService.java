package com.krishiai.auth.service;

import com.krishiai.auth.entity.PasswordResetToken;

public interface PasswordResetTokenService {

    String createResetToken(Long userId);

    PasswordResetToken validateResetToken(String rawToken);

    void markTokenUsed(PasswordResetToken token);

    void invalidateAllForUser(Long userId);
}
