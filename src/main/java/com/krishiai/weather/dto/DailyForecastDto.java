package com.krishiai.weather.dto;

public record DailyForecastDto(
        String date,
        String dayName,
        int maxTemp,
        int minTemp,
        double precipitationSum,
        double rainSum,
        int precipitationProbability,
        int maxWindSpeed,
        String sunrise,
        String sunset,
        int weatherCode,
        String condition
) {
}
