package com.krishiai.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishiai.admin.dto.UpdateUserStatusRequest;
import com.krishiai.admin.dto.UserStatusResponse;
import com.krishiai.admin.service.AdminUserService;
import com.krishiai.common.exception.GlobalExceptionHandler;
import com.krishiai.security.userdetails.CustomUserDetails;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails adminPrincipal;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@krishiai.com");
        admin.setFullName("Platform Admin");
        admin.setRole(UserRole.ROLE_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        adminPrincipal = new CustomUserDetails(admin);

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
                return adminPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status returns 200 and updated UserStatusResponse")
    void updateUserStatus_Success() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(UserStatus.BLOCKED, "Spamming forums");
        UserStatusResponse response = new UserStatusResponse(10L, "spammer@test.com", "Spam Bot",
                UserRole.ROLE_FARMER, UserStatus.BLOCKED, LocalDateTime.now());

        when(adminUserService.updateUserStatus(eq(10L), eq(request), eq(1L))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/users/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        verify(adminUserService).updateUserStatus(10L, request, 1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status returns 400 when status is missing")
    void updateUserStatus_MissingStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No status provided\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/admin/users/{userId}/block returns 200 with BLOCKED status")
    void blockUser_Success() throws Exception {
        UserStatusResponse response = new UserStatusResponse(15L, "user@test.com", "Test User",
                UserRole.ROLE_FARMER, UserStatus.BLOCKED, LocalDateTime.now());

        when(adminUserService.updateUserStatus(eq(15L), any(UpdateUserStatusRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/15/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/users/{userId}/unblock returns 200 with ACTIVE status")
    void unblockUser_Success() throws Exception {
        UserStatusResponse response = new UserStatusResponse(15L, "user@test.com", "Test User",
                UserRole.ROLE_FARMER, UserStatus.ACTIVE, LocalDateTime.now());

        when(adminUserService.updateUserStatus(eq(15L), any(UpdateUserStatusRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/users/15/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
