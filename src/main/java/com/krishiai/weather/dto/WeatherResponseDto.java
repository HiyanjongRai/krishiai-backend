package com.krishiai.weather.dto;

import java.util.List;

public record WeatherResponseDto(
        CoordinatesDto coordinates,
        LocationDto location,
        String timezone,
        CurrentWeatherDto current,
        List<HourlyForecastDto> hourly,
        List<DailyForecastDto> daily,
        WeatherMetadataDto metadata
) {
}
