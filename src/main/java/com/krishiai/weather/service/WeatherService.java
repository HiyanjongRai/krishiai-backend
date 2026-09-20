package com.krishiai.weather.service;

import com.krishiai.weather.dto.WeatherMetadataDto;
import com.krishiai.weather.dto.WeatherResponseDto;
import com.krishiai.weather.exception.WeatherException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WeatherService {

    private final String defaultProviderName;
    private final boolean fallbackEnabled;
    private final Map<String, WeatherProvider> providers;
    private final WeatherProvider fallbackProvider;

    public WeatherService(
            @Value("${krishiai.weather.provider:openweather}") String defaultProviderName,
            @Value("${krishiai.weather.fallback-enabled:true}") boolean fallbackEnabled,
            List<WeatherProvider> providerList
    ) {
        this.defaultProviderName = defaultProviderName.trim().toLowerCase();
        this.fallbackEnabled = fallbackEnabled;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toLowerCase(),
                        Function.identity()
                ));
        this.fallbackProvider = providers.get("openmeteo");
    }

    public WeatherResponseDto getWeather(Double latitude, Double longitude, String requestedProvider) {
        validateCoordinates(latitude, longitude);

        String providerKey = (requestedProvider != null && !requestedProvider.isBlank())
                ? requestedProvider.trim().toLowerCase()
                : defaultProviderName;

        WeatherProvider primary = providers.get(providerKey);
        if (primary == null) {
            log.warn("Unknown weather provider requested: '{}'. Using default: '{}'", providerKey, defaultProviderName);
            primary = providers.getOrDefault(defaultProviderName, fallbackProvider);
        }

        if (primary == null) {
            throw new WeatherException("No weather provider is configured.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            return primary.getWeather(latitude, longitude);
        } catch (Exception ex) {
            // Handle fallback if enabled and the primary provider was not already the fallback
            if (fallbackEnabled && fallbackProvider != null && !primary.equals(fallbackProvider)) {
                log.warn("Primary weather provider [{}] failed: {}. Falling back to [{}]",
                        primary.getProviderName(), ex.getMessage(), fallbackProvider.getProviderName());
                try {
                    WeatherResponseDto fallbackRes = fallbackProvider.getWeather(latitude, longitude);
                    return new WeatherResponseDto(
                            fallbackRes.coordinates(),
                            fallbackRes.location(),
                            fallbackRes.timezone(),
                            fallbackRes.current(),
                            fallbackRes.hourly(),
                            fallbackRes.daily(),
                            new WeatherMetadataDto(
                                    fallbackProvider.getProviderName() + " (fallback)",
                                    fallbackRes.metadata().fetchedAt()
                            )
                    );
                } catch (Exception fallbackEx) {
                    log.error("Fallback weather provider [{}] also failed: {}",
                            fallbackProvider.getProviderName(), fallbackEx.getMessage());
                }
            }

            if (ex instanceof WeatherException we) {
                throw we;
            }
            throw new WeatherException("Failed to retrieve weather data: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new WeatherException("Missing required query parameters: 'latitude' and 'longitude' must be provided.", HttpStatus.BAD_REQUEST);
        }
        if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new WeatherException("Invalid latitude: must be between -90.0 and 90.0.", HttpStatus.BAD_REQUEST);
        }
        if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new WeatherException("Invalid longitude: must be between -180.0 and 180.0.", HttpStatus.BAD_REQUEST);
        }
    }
}
