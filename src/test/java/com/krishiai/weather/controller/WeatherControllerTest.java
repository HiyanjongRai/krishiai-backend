package com.krishiai.weather.controller;

import com.krishiai.common.exception.GlobalExceptionHandler;
import com.krishiai.weather.dto.*;
import com.krishiai.weather.exception.WeatherException;
import com.krishiai.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController weatherController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/weather should return 200 OK with valid coordinates")
    void shouldReturnWeatherForValidCoordinates() throws Exception {
        WeatherResponseDto mockDto = new WeatherResponseDto(
                new CoordinatesDto(27.7172, 85.3240),
                new LocationDto("Kathmandu", "Bagmati", "NP"),
                "Asia/Kathmandu",
                new CurrentWeatherDto(22, 23, 55, 0.0, 0.0, 0.0, 0.0, 0, 5, 8, 120, 10, 1013, "Clear Sky", true, "2026-09-20T12:00:00Z"),
                List.of(),
                List.of(),
                new WeatherMetadataDto("openweather", "2026-09-20T12:00:00Z")
        );

        when(weatherService.getWeather(27.7172, 85.3240, null)).thenReturn(mockDto);

        mockMvc.perform(get("/api/v1/weather")
                        .param("latitude", "27.7172")
                        .param("longitude", "85.3240")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.coordinates.latitude").value(27.7172))
                .andExpect(jsonPath("$.data.coordinates.longitude").value(85.3240))
                .andExpect(jsonPath("$.data.current.temperature").value(22))
                .andExpect(jsonPath("$.data.current.condition").value("Clear Sky"))
                .andExpect(jsonPath("$.data.metadata.provider").value("openweather"));
    }

    @Test
    @DisplayName("GET /api/v1/weather should return 400 Bad Request for invalid coordinates")
    void shouldReturn400ForInvalidCoordinates() throws Exception {
        when(weatherService.getWeather(999.0, 999.0, null))
                .thenThrow(new WeatherException("Invalid latitude: must be between -90.0 and 90.0.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/api/v1/weather")
                        .param("latitude", "999.0")
                        .param("longitude", "999.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid latitude: must be between -90.0 and 90.0."));
    }

    @Test
    @DisplayName("GET /api/v1/weather should return 400 when coordinates are missing")
    void shouldReturn400WhenCoordinatesMissing() throws Exception {
        when(weatherService.getWeather(null, null, null))
                .thenThrow(new WeatherException("Missing required query parameters: 'latitude' and 'longitude' must be provided.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/api/v1/weather")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
