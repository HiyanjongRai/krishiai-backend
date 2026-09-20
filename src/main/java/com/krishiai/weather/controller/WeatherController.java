package com.krishiai.weather.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.weather.dto.WeatherResponseDto;
import com.krishiai.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@Validated
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<ApiResponse<WeatherResponseDto>> getWeather(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String provider
    ) {
        WeatherResponseDto data = weatherService.getWeather(latitude, longitude, provider);
        return ResponseEntity.ok(ApiResponse.success("Weather data retrieved successfully", data));
    }
}
