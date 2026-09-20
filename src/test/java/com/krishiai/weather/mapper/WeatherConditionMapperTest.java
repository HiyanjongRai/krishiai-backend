package com.krishiai.weather.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherConditionMapperTest {

    @Test
    @DisplayName("Should map clear sky condition for day and night")
    void shouldMapClearSky() {
        WeatherConditionMapper.ConditionResult dayClear =
                WeatherConditionMapper.mapOpenWeatherCondition(800, true, "clear sky");
        assertEquals("Clear Sky", dayClear.label());
        assertEquals(0, dayClear.weatherCode());

        WeatherConditionMapper.ConditionResult nightClear =
                WeatherConditionMapper.mapOpenWeatherCondition(800, false, "clear sky");
        assertEquals("Clear Night", nightClear.label());
        assertEquals(0, nightClear.weatherCode());
    }

    @Test
    @DisplayName("Should map cloud conditions correctly")
    void shouldMapCloudConditions() {
        WeatherConditionMapper.ConditionResult mainlyClear =
                WeatherConditionMapper.mapOpenWeatherCondition(801, true, "few clouds");
        assertEquals("Mainly Clear", mainlyClear.label());
        assertEquals(1, mainlyClear.weatherCode());

        WeatherConditionMapper.ConditionResult partlyCloudy =
                WeatherConditionMapper.mapOpenWeatherCondition(802, true, "scattered clouds");
        assertEquals("Partly Cloudy", partlyCloudy.label());
        assertEquals(2, partlyCloudy.weatherCode());

        WeatherConditionMapper.ConditionResult overcast =
                WeatherConditionMapper.mapOpenWeatherCondition(804, true, "overcast clouds");
        assertEquals("Overcast", overcast.label());
        assertEquals(3, overcast.weatherCode());
    }

    @Test
    @DisplayName("Should map rain and drizzle conditions correctly")
    void shouldMapRainAndDrizzle() {
        WeatherConditionMapper.ConditionResult drizzle =
                WeatherConditionMapper.mapOpenWeatherCondition(300, true, "light drizzle");
        assertEquals("Light Drizzle", drizzle.label());
        assertEquals(51, drizzle.weatherCode());

        WeatherConditionMapper.ConditionResult slightRain =
                WeatherConditionMapper.mapOpenWeatherCondition(500, true, "light rain");
        assertEquals("Slight Rain", slightRain.label());
        assertEquals(61, slightRain.weatherCode());

        WeatherConditionMapper.ConditionResult moderateRain =
                WeatherConditionMapper.mapOpenWeatherCondition(501, true, "moderate rain");
        assertEquals("Moderate Rain", moderateRain.label());
        assertEquals(63, moderateRain.weatherCode());

        WeatherConditionMapper.ConditionResult heavyRain =
                WeatherConditionMapper.mapOpenWeatherCondition(502, true, "heavy intensity rain");
        assertEquals("Heavy Rain", heavyRain.label());
        assertEquals(65, heavyRain.weatherCode());
    }

    @Test
    @DisplayName("Should map thunderstorm conditions correctly")
    void shouldMapThunderstorm() {
        WeatherConditionMapper.ConditionResult storm =
                WeatherConditionMapper.mapOpenWeatherCondition(211, true, "thunderstorm");
        assertEquals("Thunderstorm", storm.label());
        assertEquals(95, storm.weatherCode());

        WeatherConditionMapper.ConditionResult stormRain =
                WeatherConditionMapper.mapOpenWeatherCondition(201, true, "thunderstorm with rain");
        assertEquals("Thunderstorm with Rain", stormRain.label());
        assertEquals(96, stormRain.weatherCode());
    }

    @Test
    @DisplayName("Should map fog and atmosphere conditions correctly")
    void shouldMapAtmosphere() {
        WeatherConditionMapper.ConditionResult fog =
                WeatherConditionMapper.mapOpenWeatherCondition(741, true, "fog");
        assertEquals("Fog", fog.label());
        assertEquals(45, fog.weatherCode());

        WeatherConditionMapper.ConditionResult haze =
                WeatherConditionMapper.mapOpenWeatherCondition(721, true, "haze");
        assertEquals("Haze", haze.label());
        assertEquals(45, haze.weatherCode());
    }
}
