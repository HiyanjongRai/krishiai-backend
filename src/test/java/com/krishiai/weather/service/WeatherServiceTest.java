package com.krishiai.weather.service;

import com.krishiai.weather.dto.*;
import com.krishiai.weather.exception.WeatherException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherProvider openWeatherProvider;

    @Mock
    private WeatherProvider openMeteoProvider;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        lenient().when(openWeatherProvider.getProviderName()).thenReturn("openweather");
        lenient().when(openMeteoProvider.getProviderName()).thenReturn("openmeteo");

        weatherService = new WeatherService(
                "openweather",
                true,
                List.of(openWeatherProvider, openMeteoProvider)
        );
    }

    @Test
    @DisplayName("Should reject null coordinates with BAD_REQUEST")
    void shouldRejectNullCoordinates() {
        WeatherException ex1 = assertThrows(WeatherException.class, () ->
                weatherService.validateCoordinates(null, 85.32)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());

        WeatherException ex2 = assertThrows(WeatherException.class, () ->
                weatherService.validateCoordinates(27.71, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());
    }

    @Test
    @DisplayName("Should reject out-of-range coordinates with BAD_REQUEST")
    void shouldRejectOutOfRangeCoordinates() {
        WeatherException exLatHigh = assertThrows(WeatherException.class, () ->
                weatherService.validateCoordinates(90.1, 85.32)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exLatHigh.getStatus());

        WeatherException exLatLow = assertThrows(WeatherException.class, () ->
                weatherService.validateCoordinates(-90.1, 85.32)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exLatLow.getStatus());

        WeatherException exLonHigh = assertThrows(WeatherException.class, () ->
                weatherService.validateCoordinates(27.71, 180.1)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exLonHigh.getStatus());

        WeatherException exLonLow = assertThrows(WeatherException.class, () ->
                weatherService.validateCoordinates(27.71, -180.1)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exLonLow.getStatus());
    }

    @Test
    @DisplayName("Should accept valid coordinates without throwing exception")
    void shouldAcceptValidCoordinates() {
        assertDoesNotThrow(() -> weatherService.validateCoordinates(27.7172, 85.3240));
        assertDoesNotThrow(() -> weatherService.validateCoordinates(0.0, 0.0));
        assertDoesNotThrow(() -> weatherService.validateCoordinates(-90.0, -180.0));
        assertDoesNotThrow(() -> weatherService.validateCoordinates(90.0, 180.0));
    }

    @Test
    @DisplayName("Should return primary provider response when primary succeeds")
    void shouldReturnPrimaryResponseWhenSuccessful() {
        WeatherResponseDto mockResponse = createDummyResponse("openweather");
        when(openWeatherProvider.getWeather(27.71, 85.32)).thenReturn(mockResponse);

        WeatherResponseDto result = weatherService.getWeather(27.71, 85.32, null);

        assertNotNull(result);
        assertEquals("openweather", result.metadata().provider());
        verify(openWeatherProvider, times(1)).getWeather(27.71, 85.32);
        verify(openMeteoProvider, never()).getWeather(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should gracefully fallback to Open-Meteo when OpenWeather fails")
    void shouldFallbackWhenPrimaryFails() {
        when(openWeatherProvider.getWeather(27.71, 85.32))
                .thenThrow(new WeatherException("OpenWeather 401 Unauthorized", HttpStatus.BAD_GATEWAY));

        WeatherResponseDto fallbackResponse = createDummyResponse("openmeteo");
        when(openMeteoProvider.getWeather(27.71, 85.32)).thenReturn(fallbackResponse);

        WeatherResponseDto result = weatherService.getWeather(27.71, 85.32, null);

        assertNotNull(result);
        assertEquals("openmeteo (fallback)", result.metadata().provider());
        verify(openWeatherProvider, times(1)).getWeather(27.71, 85.32);
        verify(openMeteoProvider, times(1)).getWeather(27.71, 85.32);
    }

    private WeatherResponseDto createDummyResponse(String provider) {
        return new WeatherResponseDto(
                new CoordinatesDto(27.71, 85.32),
                new LocationDto("Kathmandu", "Bagmati", "NP"),
                "Asia/Kathmandu",
                new CurrentWeatherDto(24, 25, 60, 0.0, 0.0, 0.0, 0.0, 0, 10, 12, 180, 15, 1013, "Clear Sky", true, "2026-09-20T12:00:00Z"),
                List.of(),
                List.of(),
                new WeatherMetadataDto(provider, "2026-09-20T12:00:00Z")
        );
    }
}
