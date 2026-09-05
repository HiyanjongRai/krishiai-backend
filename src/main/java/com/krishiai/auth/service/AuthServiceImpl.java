package com.krishiai.auth.service;

import com.krishiai.auth.dto.LoginRequest;
import com.krishiai.auth.dto.LoginResponse;
import com.krishiai.auth.dto.RegisterRequest;
import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ConflictException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.UnauthorizedException;
import com.krishiai.expert.service.ExpertProfileService;
import com.krishiai.security.jwt.JwtTokenProvider;
import com.krishiai.user.dto.UserResponse;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.entity.UserStatus;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // @Lazy prevents a circular dependency: AuthService → ExpertProfileService → UserRepository → AuthService
    @Lazy
    @Autowired
    private ExpertProfileService expertProfileService;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = User.normaliseEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with email " + normalizedEmail + " already exists");
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            String cleanPhone = request.phone().strip();
            if (userRepository.existsByPhone(cleanPhone)) {
                throw new ConflictException("An account with phone number " + cleanPhone + " already exists");
            }
        }

        if (request.role() != null && request.role() == UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Public registration as administrator is strictly prohibited");
        }

        UserRole role = (request.role() != null) ? request.role() : UserRole.ROLE_FARMER;
        if (role != UserRole.ROLE_FARMER && role != UserRole.ROLE_EXPERT) {
            throw new BadRequestException("Invalid registration role. Only FARMER and EXPERT roles are allowed.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().strip());
        user.setLastName(request.lastName().strip());
        user.setPhone(request.phone() != null && !request.phone().isBlank() ? request.phone().strip() : null);
        user.setRole(role);
        // All registered users (farmers & experts) start ACTIVE to allow dashboard & onboarding access.
        // Professional verification status is managed separately on ExpertProfile.
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);
        log.info("Registered new user for KrishiAI: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        // Auto-create the ExpertProfile immediately so the expert can log in and find their profile
        if (savedUser.getRole() == UserRole.ROLE_EXPERT) {
            expertProfileService.ensureProfileExists(savedUser.getId());
            log.info("Auto-created ExpertProfile for new expert: {}", savedUser.getEmail());
        }

        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        String normalizedEmail = User.normaliseEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ForbiddenException("Account is temporarily locked due to repeated failed login attempts. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            if (user.getFailedLoginAttempts() >= 5) {
                user.lockUntil(LocalDateTime.now().plusMinutes(15));
                log.warn("Account locked for 15 minutes due to 5 failed login attempts: {}", user.getEmail());
            }
            userRepository.save(user);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check account status
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Your account has been suspended. Please contact support.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ForbiddenException("Your account is currently inactive. Please contact support.");
        }

        // ROLE_EXPERT users may have UserStatus.PENDING while their professional verification
        // is in progress. They must be allowed to log in so they can complete their profile
        // and submit their verification application. The PENDING guard below only applies
        // to non-expert roles (e.g. ROLE_FARMER pending email verification).
        if (user.getStatus() == UserStatus.PENDING && user.getRole() != UserRole.ROLE_EXPERT) {
            throw new ForbiddenException("Your account is pending verification. Please check your email.");
        }

        user.recordSuccessfulLogin(clientIp);
        userRepository.save(user);

        log.info("User authenticated successfully: {} (Role: {})", user.getEmail(), user.getRole());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        return LoginResponse.of(token, jwtExpirationMs, UserResponse.from(user));
    }
}
