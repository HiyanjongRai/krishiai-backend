package com.krishiai.weather.service;

import com.krishiai.weather.dto.WeatherResponseDto;

public interface WeatherProvider {

    String getProviderName();

    WeatherResponseDto getWeather(double latitude, double longitude);
}
