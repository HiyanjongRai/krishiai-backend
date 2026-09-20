package com.krishiai.weather.exception;

import org.springframework.http.HttpStatus;

public class WeatherException extends RuntimeException {

    private final HttpStatus status;

    public WeatherException(String message) {
        super(message);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }

    public WeatherException(String message, HttpStatus status) {
        super(message);
        this.status = status != null ? status : HttpStatus.SERVICE_UNAVAILABLE;
    }

    public WeatherException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status != null ? status : HttpStatus.SERVICE_UNAVAILABLE;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
