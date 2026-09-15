package com.krishiai.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long AUTH_WINDOW_MS = 5 * 60 * 1000L;
    private static final int AUTH_MAX_REQUESTS = 20;
    private static final long MEDIA_WINDOW_MS = 60 * 1000L;
    private static final int MEDIA_MAX_REQUESTS = 30;

    private final Map<String, ArrayDeque<Long>> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Limit limit = resolveLimit(request);
        if (limit != null && !allow(request, limit)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"status\":429,\"message\":\"Too many requests. Please wait before trying again.\",\"timestamp\":\"%s\",\"path\":\"%s\",\"errors\":null}",
                    LocalDateTime.now(),
                    request.getRequestURI()
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Limit resolveLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/auth/")) {
            return new Limit(AUTH_WINDOW_MS, AUTH_MAX_REQUESTS);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/media/upload".equals(path)) {
            return new Limit(MEDIA_WINDOW_MS, MEDIA_MAX_REQUESTS);
        }
        return null;
    }

    private boolean allow(HttpServletRequest request, Limit limit) {
        String key = clientIp(request) + ":" + request.getMethod() + ":" + request.getRequestURI();
        long now = System.currentTimeMillis();
        ArrayDeque<Long> timestamps = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > limit.windowMs()) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= limit.maxRequests()) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Limit(long windowMs, int maxRequests) {}
}
