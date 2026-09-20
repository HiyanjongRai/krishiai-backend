package com.krishiai.weather.dto;

public record WeatherMetadataDto(
        String provider,
        String fetchedAt
) {
}
