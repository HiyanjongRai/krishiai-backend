package com.krishiai.weather.dto;

public record CurrentWeatherDto(
        int temperature,
        int apparentTemperature,
        int relativeHumidity,
        double precipitation,
        double rain,
        double showers,
        double snowfall,
        int weatherCode,
        int cloudCover,
        int windSpeed,
        int windDirection,
        int windGusts,
        Integer pressure,
        String condition,
        boolean isDay,
        String time
) {
}
