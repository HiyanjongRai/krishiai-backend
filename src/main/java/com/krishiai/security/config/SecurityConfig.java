package com.krishiai.security.config;

import com.krishiai.security.jwt.JwtAuthenticationFilter;
import com.krishiai.security.ratelimit.ApiRateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(parseAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(String.format(
                                    "{\"status\":401,\"message\":\"Full authentication is required to access this resource\",\"timestamp\":\"%s\",\"path\":\"%s\",\"errors\":null}",
                                    LocalDateTime.now(),
                                    request.getRequestURI()
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(String.format(
                                    "{\"status\":403,\"message\":\"Access denied: you do not have permission to access this resource\",\"timestamp\":\"%s\",\"path\":\"%s\",\"errors\":null}",
                                    LocalDateTime.now(),
                                    request.getRequestURI()
                            ));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // Permit all OPTIONS preflight requests from browser
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public authentication endpoints (no Bearer token required)
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/forgot-password").permitAll()
                        .requestMatchers("/api/v1/auth/reset-password").permitAll()
                        // /api/v1/auth/logout and /api/v1/auth/change-password require authentication

                        // Public read-only catalog and knowledge
                        .requestMatchers(HttpMethod.GET, "/api/v1/crops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/crop-categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/experts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/weather/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/locations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/specializations/**").permitAll()

                        // Error endpoint
                        .requestMatchers("/error").permitAll()

                        // WebSocket handshake endpoint (JWT auth is enforced by STOMP interceptor)
                        .requestMatchers("/ws/**").permitAll()

                        // Admin routes require ROLE_ADMIN
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")

                        // Expert routes require ROLE_EXPERT. Admin management must use /api/v1/admin/**.
                        .requestMatchers("/api/v1/expert/**").hasAuthority("ROLE_EXPERT")

                        // Consultation packages - public GET listing, mutations are expert-only (checked via @PreAuthorize)
                        .requestMatchers(HttpMethod.GET, "/api/v1/experts/*/consultation-packages").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/consultation-packages/**").permitAll()

                        // Payments - farmers initiate/verify; experts/admin check status
                        .requestMatchers("/api/v1/payments/**").hasAnyAuthority("ROLE_FARMER", "ROLE_EXPERT", "ROLE_ADMIN")

                        // Consultation messages are participant-checked in the service layer.
                        .requestMatchers("/api/v1/consultations/**").hasAnyAuthority("ROLE_FARMER", "ROLE_EXPERT", "ROLE_ADMIN")

                        // Messaging system - membership checked in service
                        .requestMatchers("/api/v1/conversations/**").authenticated()
                        .requestMatchers("/api/v1/messages/**").authenticated()
                        .requestMatchers("/api/v1/announcements/**").authenticated()

                        // Farmer routes require ROLE_FARMER
                        .requestMatchers("/api/v1/farmer/**").hasAuthority("ROLE_FARMER")

                        // All other API requests must be authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(apiRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private List<String> parseAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .toList();
    }
}
