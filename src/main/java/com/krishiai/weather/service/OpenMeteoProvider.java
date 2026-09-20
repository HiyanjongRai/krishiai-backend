package com.krishiai.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishiai.weather.dto.*;
import com.krishiai.weather.exception.WeatherException;
import com.krishiai.weather.mapper.WeatherConditionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Component
public class OpenMeteoProvider implements WeatherProvider {

    private final String baseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenMeteoProvider(
            @Value("${krishiai.weather.openmeteo.base-url:https://api.open-meteo.com}") String baseUrl,
            @Value("${krishiai.weather.timeout-seconds:8}") int timeoutSeconds
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(this.baseUrl)
                .build();
    }

    @Override
    public String getProviderName() {
        return "openmeteo";
    }

    @Override
    public WeatherResponseDto getWeather(double latitude, double longitude) {
        String currentParams = String.join(",",
                "temperature_2m", "apparent_temperature", "relative_humidity_2m",
                "precipitation", "rain", "showers", "snowfall", "weather_code",
                "cloud_cover", "wind_speed_10m", "wind_direction_10m", "wind_gusts_10m", "is_day"
        );

        String hourlyParams = String.join(",",
                "temperature_2m", "relative_humidity_2m", "precipitation_probability",
                "precipitation", "rain", "weather_code", "wind_speed_10m", "cloud_cover"
        );

        String dailyParams = String.join(",",
                "temperature_2m_max", "temperature_2m_min", "precipitation_sum",
                "rain_sum", "precipitation_probability_max", "wind_speed_10m_max",
                "sunrise", "sunset", "weather_code"
        );

        String uri = String.format(
                Locale.ROOT,
                "/v1/forecast?latitude=%.4f&longitude=%.4f&current=%s&hourly=%s&daily=%s&timezone=auto&forecast_days=7",
                latitude, longitude, currentParams, hourlyParams, dailyParams
        );

        JsonNode root;
        try {
            String bodyStr = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            if (bodyStr == null || bodyStr.isBlank()) {
                throw new WeatherException("Empty response received from Open-Meteo.", HttpStatus.BAD_GATEWAY);
            }
            root = objectMapper.readTree(bodyStr);
        } catch (WeatherException we) {
            throw we;
        } catch (ResourceAccessException rae) {
            log.warn("Open-Meteo timeout: {}", rae.getMessage());
            throw new WeatherException("Open-Meteo request timed out.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception ex) {
            log.error("Open-Meteo request failed: {}", ex.getMessage());
            throw new WeatherException("Open-Meteo service error: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (root == null || !root.has("current") || !root.has("daily") || !root.has("hourly")) {
            throw new WeatherException("Incomplete response received from Open-Meteo", HttpStatus.BAD_GATEWAY);
        }

        return mapToWeatherResponse(latitude, longitude, root);
    }

    private WeatherResponseDto mapToWeatherResponse(double latitude, double longitude, JsonNode root) {
        CoordinatesDto coordinates = new CoordinatesDto(latitude, longitude);
        LocationDto location = new LocationDto("", "", "");
        String timezone = root.path("timezone").asText("auto");

        // --- Current ---
        JsonNode cur = root.path("current");
        boolean isDay = cur.path("is_day").asInt(1) == 1;
        int weatherCode = cur.path("weather_code").asInt(0);
        String condition = getWmoLabel(weatherCode, isDay);

        CurrentWeatherDto current = new CurrentWeatherDto(
                (int) Math.round(cur.path("temperature_2m").asDouble(0.0)),
                (int) Math.round(cur.path("apparent_temperature").asDouble(0.0)),
                cur.path("relative_humidity_2m").asInt(0),
                cur.path("precipitation").asDouble(0.0),
                cur.path("rain").asDouble(0.0),
                cur.path("showers").asDouble(0.0),
                cur.path("snowfall").asDouble(0.0),
                weatherCode,
                cur.path("cloud_cover").asInt(0),
                (int) Math.round(cur.path("wind_speed_10m").asDouble(0.0)),
                cur.path("wind_direction_10m").asInt(0),
                (int) Math.round(cur.path("wind_gusts_10m").asDouble(cur.path("wind_speed_10m").asDouble(0.0))),
                1013,
                condition,
                isDay,
                cur.path("time").asText("")
        );

        // --- Hourly ---
        List<HourlyForecastDto> hourly = new ArrayList<>();
        JsonNode hourlyNode = root.path("hourly");
        JsonNode hourlyTimes = hourlyNode.path("time");
        String curTime = cur.path("time").asText("");
        int startIndex = 0;
        if (hourlyTimes.isArray() && !curTime.isEmpty()) {
            String curPrefix = curTime.length() >= 13 ? curTime.substring(0, 13) : curTime;
            for (int i = 0; i < hourlyTimes.size(); i++) {
                if (hourlyTimes.get(i).asText("").startsWith(curPrefix)) {
                    startIndex = i;
                    break;
                }
            }
        }

        int count = 0;
        int maxSlots = 24;
        if (hourlyTimes.isArray()) {
            for (int i = startIndex; i < hourlyTimes.size() && count < maxSlots; i++, count++) {
                String t = hourlyTimes.get(i).asText("");
                int code = hourlyNode.path("weather_code").path(i).asInt(0);
                String hourCond = getWmoLabel(code, true);

                String displayTime = count == 0 ? "Now" :
                        (t.length() >= 16 ? t.substring(11, 16) : t);

                hourly.add(new HourlyForecastDto(
                        t,
                        displayTime,
                        (int) Math.round(hourlyNode.path("temperature_2m").path(i).asDouble(0.0)),
                        (int) Math.round(hourlyNode.path("temperature_2m").path(i).asDouble(0.0)),
                        hourlyNode.path("relative_humidity_2m").path(i).asInt(0),
                        hourlyNode.path("precipitation_probability").path(i).asInt(0),
                        hourlyNode.path("precipitation").path(i).asDouble(0.0),
                        hourlyNode.path("rain").path(i).asDouble(0.0),
                        code,
                        hourCond,
                        (int) Math.round(hourlyNode.path("wind_speed_10m").path(i).asDouble(0.0)),
                        hourlyNode.path("cloud_cover").path(i).asInt(0)
                ));
            }
        }

        // --- Daily ---
        List<DailyForecastDto> daily = new ArrayList<>();
        JsonNode dailyNode = root.path("daily");
        JsonNode dailyTimes = dailyNode.path("time");
        if (dailyTimes.isArray()) {
            for (int i = 0; i < dailyTimes.size(); i++) {
                String dateStr = dailyTimes.get(i).asText("");
                int code = dailyNode.path("weather_code").path(i).asInt(0);
                String dayCond = getWmoLabel(code, true);

                String dayName;
                if (i == 0) {
                    dayName = "Today";
                } else if (i == 1) {
                    dayName = "Tomorrow";
                } else {
                    try {
                        LocalDate d = LocalDate.parse(dateStr);
                        dayName = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    } catch (Exception e) {
                        dayName = dateStr;
                    }
                }

                daily.add(new DailyForecastDto(
                        dateStr,
                        dayName,
                        (int) Math.round(dailyNode.path("temperature_2m_max").path(i).asDouble(0.0)),
                        (int) Math.round(dailyNode.path("temperature_2m_min").path(i).asDouble(0.0)),
                        Math.round(dailyNode.path("precipitation_sum").path(i).asDouble(0.0) * 10.0) / 10.0,
                        Math.round(dailyNode.path("rain_sum").path(i).asDouble(0.0) * 10.0) / 10.0,
                        dailyNode.path("precipitation_probability_max").path(i).asInt(0),
                        (int) Math.round(dailyNode.path("wind_speed_10m_max").path(i).asDouble(0.0)),
                        dailyNode.path("sunrise").path(i).asText(""),
                        dailyNode.path("sunset").path(i).asText(""),
                        code,
                        dayCond
                ));
            }
        }

        WeatherMetadataDto metadata = new WeatherMetadataDto(
                "openmeteo",
                Instant.now().toString()
        );

        return new WeatherResponseDto(
                coordinates,
                location,
                timezone,
                current,
                hourly,
                daily,
                metadata
        );
    }

    private static String getWmoLabel(int code, boolean isDay) {
        return switch (code) {
            case 0 -> isDay ? "Clear Sky" : "Clear Night";
            case 1 -> "Mainly Clear";
            case 2 -> "Partly Cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51 -> "Light Drizzle";
            case 53 -> "Moderate Drizzle";
            case 55 -> "Dense Drizzle";
            case 56, 57 -> "Freezing Drizzle";
            case 61 -> "Slight Rain";
            case 63 -> "Moderate Rain";
            case 65 -> "Heavy Rain";
            case 66, 67 -> "Freezing Rain";
            case 71 -> "Slight Snow";
            case 73 -> "Moderate Snow";
            case 75 -> "Heavy Snow";
            case 77 -> "Snow Grains";
            case 80 -> "Slight Showers";
            case 81 -> "Moderate Showers";
            case 82 -> "Violent Showers";
            case 85, 86 -> "Snow Showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with Hail";
            default -> "Fair";
        };
    }
}
