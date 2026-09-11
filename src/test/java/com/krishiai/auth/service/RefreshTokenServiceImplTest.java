package com.krishiai.auth.service;

import com.krishiai.auth.entity.RefreshToken;
import com.krishiai.auth.repository.RefreshTokenRepository;
import com.krishiai.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 2592000000L);
    }

    @Test
    @DisplayName("createRefreshToken revokes existing tokens, saves new hashed token, and returns raw token")
    void createRefreshToken_Success() {
        Long userId = 100L;
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(userId);

        assertThat(rawToken).isNotBlank();
        verify(refreshTokenRepository).revokeAllByUserId(userId);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken); // Stored as hash, not raw
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("validateRefreshToken returns valid token")
    void validateRefreshToken_Success() {
        String rawToken = "sample-token-1234";
        RefreshToken token = new RefreshToken("dummy-hash", 100L, LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.validateRefreshToken(rawToken);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("validateRefreshToken throws UnauthorizedException when token not found")
    void validateRefreshToken_NotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("unknown-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or unknown refresh token");
    }

    @Test
    @DisplayName("validateRefreshToken throws UnauthorizedException when token is revoked")
    void validateRefreshToken_Revoked() {
        RefreshToken token = new RefreshToken("dummy-hash", 100L, LocalDateTime.now().plusDays(7));
        token.revoke();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("revoked-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("validateRefreshToken throws UnauthorizedException when token is expired")
    void validateRefreshToken_Expired() {
        RefreshToken token = new RefreshToken("dummy-hash", 100L, LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("expired-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("revokeRefreshToken marks token as revoked")
    void revokeRefreshToken_Success() {
        RefreshToken token = new RefreshToken("dummy-hash", 100L, LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        refreshTokenService.revokeRefreshToken("raw-token");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("revokeAllForUser delegates to repository")
    void revokeAllForUser_Success() {
        when(refreshTokenRepository.revokeAllByUserId(100L)).thenReturn(2);

        refreshTokenService.revokeAllForUser(100L);

        verify(refreshTokenRepository).revokeAllByUserId(100L);
    }
}
