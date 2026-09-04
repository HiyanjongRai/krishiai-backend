package com.krishiai.auth.service;

import com.krishiai.auth.dto.LoginRequest;
import com.krishiai.auth.dto.LoginResponse;
import com.krishiai.auth.dto.RegisterRequest;
import com.krishiai.user.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request, String clientIp);
}
