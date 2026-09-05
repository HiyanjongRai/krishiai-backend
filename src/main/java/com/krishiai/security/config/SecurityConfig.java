package com.krishiai.security.config;

import com.krishiai.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

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

                        // Public authentication endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Public read-only catalog and knowledge
                        .requestMatchers(HttpMethod.GET, "/api/v1/crops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/experts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/weather/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/locations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/specializations/**").permitAll()

                        // Error endpoint
                        .requestMatchers("/error").permitAll()

                        // Admin routes require ROLE_ADMIN
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")

                        // Expert routes require ROLE_EXPERT (or ROLE_ADMIN for preview/management)
                        .requestMatchers("/api/v1/expert/**").hasAnyAuthority("ROLE_EXPERT", "ROLE_ADMIN")

                        // All other API requests must be authenticated
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
