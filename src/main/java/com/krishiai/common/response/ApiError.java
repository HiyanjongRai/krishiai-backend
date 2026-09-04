package com.krishiai.common.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API error payload structure returned on exceptions.
 */
public record ApiError(
        int status,
        String message,
        LocalDateTime timestamp,
        String path,
        Map<String, String> errors
) {
}
