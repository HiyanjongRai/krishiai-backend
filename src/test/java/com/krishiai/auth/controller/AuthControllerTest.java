package com.krishiai.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishiai.auth.dto.ChangePasswordRequest;
import com.krishiai.auth.dto.LoginRequest;
import com.krishiai.auth.dto.RefreshTokenRequest;
import com.krishiai.auth.dto.TokenResponse;
import com.krishiai.auth.service.AuthService;
import com.krishiai.common.exception.GlobalExceptionHandler;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.dto.UserResponse;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails mockPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.createFarmer("farmer@krishiai.com", "hash", "John", "Doe", "+9779801234567");
        user.setId(42L);
        mockPrincipal = new CustomUserDetails(user);

        // HandlerMethodArgumentResolver to resolve @AuthenticationPrincipal CustomUserDetails
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return mockPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 and TokenResponse on valid credentials")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("farmer@krishiai.com", "Password@123");
        UserResponse userResponse = new UserResponse(42L, "farmer@krishiai.com", "John Doe",
                "+9779801234567", null, UserRole.ROLE_FARMER, UserStatus.ACTIVE, true, null, LocalDateTime.now());
        TokenResponse tokenResponse = TokenResponse.of("access-token-xyz", 86400000L, "refresh-token-abc", userResponse);

        when(authService.login(eq(request), any())).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-xyz"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-abc"))
                .andExpect(jsonPath("$.data.user.email").value("farmer@krishiai.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 400 when email or password blank")
    void login_InvalidRequest() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh returns 200 and new TokenResponse on valid refresh token")
    void refresh_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("uuid-refresh-token");
        UserResponse userResponse = new UserResponse(42L, "farmer@krishiai.com", "John Doe",
                "+9779801234567", null, UserRole.ROLE_FARMER, UserStatus.ACTIVE, true, null, LocalDateTime.now());
        TokenResponse tokenResponse = TokenResponse.of("new-access-token", 86400000L, "new-refresh-token", userResponse);

        when(authService.refreshToken(request)).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh returns 400 when refreshToken is blank")
    void refresh_BlankToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("   ");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout invokes authService.logout with principal user ID")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout(42L);
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password returns 200 when request is valid")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword@123", "NewPassword@456");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(authService).changePassword(42L, request);
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password returns 400 when new password is too short")
    void changePassword_ShortPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword@123", "short");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
