package com.krishiai.weather.dto;

public record HourlyForecastDto(
        String time,
        String displayTime,
        int temperature,
        int apparentTemperature,
        int relativeHumidity,
        int precipitationProbability,
        double precipitation,
        double rain,
        int weatherCode,
        String condition,
        int windSpeed,
        int cloudCover
) {
}
