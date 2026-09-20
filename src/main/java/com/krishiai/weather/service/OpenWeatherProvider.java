package com.krishiai.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishiai.weather.dto.*;
import com.krishiai.weather.exception.WeatherException;
import com.krishiai.weather.mapper.WeatherConditionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Component
public class OpenWeatherProvider implements WeatherProvider {

    private final String apiKey;
    private final String baseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenWeatherProvider(
            @Value("${krishiai.weather.openweather.api-key:}") String apiKey,
            @Value("${krishiai.weather.openweather.base-url:https://api.openweathermap.org}") String baseUrl,
            @Value("${krishiai.weather.timeout-seconds:8}") int timeoutSeconds
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
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
        return "openweather";
    }

    @Override
    public WeatherResponseDto getWeather(double latitude, double longitude) {
        if (apiKey.isBlank()) {
            throw new WeatherException("OpenWeather API key is not configured.", HttpStatus.BAD_GATEWAY);
        }

        // 1. Fetch Current Weather
        JsonNode currentRoot = fetchCurrentWeather(latitude, longitude);

        // 2. Fetch 5-Day / 3-Hour Forecast
        JsonNode forecastRoot = fetchForecast(latitude, longitude);

        // 3. Normalize into KrishiAI WeatherResponseDto
        return mapToWeatherResponse(latitude, longitude, currentRoot, forecastRoot);
    }

    private JsonNode fetchCurrentWeather(double latitude, double longitude) {
        String uri = String.format(Locale.ROOT, "/data/2.5/weather?lat=%.4f&lon=%.4f&units=metric&appid=%s",
                latitude, longitude, apiKey);

        return executeRequest(uri, "current weather");
    }

    private JsonNode fetchForecast(double latitude, double longitude) {
        String uri = String.format(Locale.ROOT, "/data/2.5/forecast?lat=%.4f&lon=%.4f&units=metric&appid=%s",
                latitude, longitude, apiKey);

        return executeRequest(uri, "forecast");
    }

    private JsonNode executeRequest(String uri, String operationName) {
        try {
            String bodyStr = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        int code = res.getStatusCode().value();
                        if (code == 401 || code == 403) {
                            log.error("OpenWeather authentication failed for {} (HTTP {})", operationName, code);
                            throw new WeatherException("OpenWeather authentication failed. Please verify API key.", HttpStatus.BAD_GATEWAY);
                        } else if (code == 429) {
                            log.warn("OpenWeather rate limit exceeded for {}", operationName);
                            throw new WeatherException("OpenWeather rate limit exceeded. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
                        } else {
                            log.warn("OpenWeather client error for {} (HTTP {})", operationName, code);
                            throw new WeatherException("Invalid request to OpenWeather service.", HttpStatus.BAD_REQUEST);
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("OpenWeather server error for {} (HTTP {})", operationName, res.getStatusCode().value());
                        throw new WeatherException("OpenWeather service is currently experiencing upstream issues.", HttpStatus.SERVICE_UNAVAILABLE);
                    })
                    .body(String.class);

            if (bodyStr == null || bodyStr.isBlank()) {
                throw new WeatherException("Empty response received from OpenWeather.", HttpStatus.BAD_GATEWAY);
            }
            return objectMapper.readTree(bodyStr);
        } catch (WeatherException we) {
            throw we;
        } catch (ResourceAccessException rae) {
            log.warn("OpenWeather request timed out or network error during {}: {}", operationName, rae.getMessage());
            throw new WeatherException("OpenWeather request timed out. Please try again.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception ex) {
            log.error("Unexpected error contacting OpenWeather for {}: {}", operationName, ex.getMessage());
            throw new WeatherException("Unable to fetch weather data from OpenWeather.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private WeatherResponseDto mapToWeatherResponse(
            double latitude,
            double longitude,
            JsonNode currentRoot,
            JsonNode forecastRoot
    ) {
        // --- Coordinates & Location ---
        String cityName = currentRoot.path("name").asText("");
        String country = currentRoot.path("sys").path("country").asText("");
        LocationDto location = new LocationDto(cityName, "", country);
        CoordinatesDto coordinates = new CoordinatesDto(latitude, longitude);

        int timezoneOffsetSeconds = currentRoot.path("timezone").asInt(0);
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds);
        String timezoneStr = "UTC" + (zoneOffset.getTotalSeconds() >= 0 ? "+" : "") + zoneOffset.getId();

        // --- Current Weather ---
        JsonNode main = currentRoot.path("main");
        JsonNode wind = currentRoot.path("wind");
        JsonNode clouds = currentRoot.path("clouds");
        JsonNode weatherArr = currentRoot.path("weather");
        JsonNode sys = currentRoot.path("sys");

        int temp = (int) Math.round(main.path("temp").asDouble(0.0));
        int feelsLike = (int) Math.round(main.path("feels_like").asDouble(temp));
        int humidity = main.path("humidity").asInt(0);
        int pressure = main.path("pressure").asInt(1013);

        // OpenWeather wind speed is in m/s with units=metric. KrishiAI requires km/h (km/h = m/s * 3.6).
        double windSpeedMs = wind.path("speed").asDouble(0.0);
        int windSpeedKmH = (int) Math.round(windSpeedMs * 3.6);
        int windDeg = wind.path("deg").asInt(0);
        double windGustMs = wind.path("gust").asDouble(windSpeedMs);
        int windGustKmH = (int) Math.round(windGustMs * 3.6);

        int cloudCover = clouds.path("all").asInt(0);

        double rain1h = currentRoot.path("rain").path("1h").asDouble(0.0);
        if (rain1h == 0.0) {
            rain1h = currentRoot.path("rain").path("3h").asDouble(0.0) / 3.0;
        }
        double snow1h = currentRoot.path("snow").path("1h").asDouble(0.0);
        double precipitation = +(rain1h + snow1h);

        long dt = currentRoot.path("dt").asLong(Instant.now().getEpochSecond());
        long sunrise = sys.path("sunrise").asLong(0);
        long sunset = sys.path("sunset").asLong(0);

        boolean isDay = (sunrise == 0 || sunset == 0)
                ? (LocalTime.now(zoneOffset).getHour() >= 6 && LocalTime.now(zoneOffset).getHour() < 18)
                : (dt >= sunrise && dt < sunset);

        int weatherId = 800;
        String weatherDesc = "Clear";
        if (weatherArr.isArray() && !weatherArr.isEmpty()) {
            JsonNode w0 = weatherArr.get(0);
            weatherId = w0.path("id").asInt(800);
            weatherDesc = w0.path("description").asText("Clear");
        }

        WeatherConditionMapper.ConditionResult conditionResult =
                WeatherConditionMapper.mapOpenWeatherCondition(weatherId, isDay, weatherDesc);

        CurrentWeatherDto current = new CurrentWeatherDto(
                temp,
                feelsLike,
                humidity,
                precipitation,
                rain1h,
                0.0,
                snow1h,
                conditionResult.weatherCode(),
                cloudCover,
                windSpeedKmH,
                windDeg,
                windGustKmH,
                pressure,
                conditionResult.label(),
                isDay,
                Instant.ofEpochSecond(dt).atOffset(zoneOffset).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );

        // --- Forecast (Hourly Intervals & Daily Aggregations) ---
        List<HourlyForecastDto> hourly = new ArrayList<>();
        Map<String, List<JsonNode>> dayBuckets = new LinkedHashMap<>();

        JsonNode listNode = forecastRoot.path("list");
        if (listNode.isArray()) {
            int slotCount = 0;
            for (JsonNode item : listNode) {
                long itemDt = item.path("dt").asLong(0);
                OffsetDateTime itemTime = Instant.ofEpochSecond(itemDt).atOffset(zoneOffset);
                String dateKey = itemTime.toLocalDate().toString();

                dayBuckets.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(item);

                // Include next 8 intervals (24 hours) as the hourly forecast representation
                if (slotCount < 8) {
                    JsonNode itemMain = item.path("main");
                    JsonNode itemWind = item.path("wind");
                    JsonNode itemWeatherArr = item.path("weather");

                    int itemTemp = (int) Math.round(itemMain.path("temp").asDouble(0.0));
                    int itemFeelsLike = (int) Math.round(itemMain.path("feels_like").asDouble(itemTemp));
                    int itemHumidity = itemMain.path("humidity").asInt(0);
                    // pop is probability of precipitation in range 0.0 - 1.0. Convert to %
                    int popPercent = (int) Math.round(item.path("pop").asDouble(0.0) * 100);
                    double itemRain3h = item.path("rain").path("3h").asDouble(0.0);

                    double itemWindSpeedMs = itemWind.path("speed").asDouble(0.0);
                    int itemWindSpeedKmH = (int) Math.round(itemWindSpeedMs * 3.6);
                    int itemCloudCover = item.path("clouds").path("all").asInt(0);

                    boolean itemIsDay = "d".equalsIgnoreCase(item.path("sys").path("pod").asText("d"));

                    int itemWId = 800;
                    String itemWDesc = "Clear";
                    if (itemWeatherArr.isArray() && !itemWeatherArr.isEmpty()) {
                        itemWId = itemWeatherArr.get(0).path("id").asInt(800);
                        itemWDesc = itemWeatherArr.get(0).path("description").asText("Clear");
                    }

                    WeatherConditionMapper.ConditionResult itemCond =
                            WeatherConditionMapper.mapOpenWeatherCondition(itemWId, itemIsDay, itemWDesc);

                    String displayTime = slotCount == 0 ? "Now" :
                            itemTime.format(DateTimeFormatter.ofPattern("HH:mm"));

                    hourly.add(new HourlyForecastDto(
                            itemTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            displayTime,
                            itemTemp,
                            itemFeelsLike,
                            itemHumidity,
                            popPercent,
                            itemRain3h,
                            itemRain3h,
                            itemCond.weatherCode(),
                            itemCond.label(),
                            itemWindSpeedKmH,
                            itemCloudCover
                    ));
                    slotCount++;
                }
            }
        }

        // --- Daily Forecast Aggregation ---
        List<DailyForecastDto> daily = new ArrayList<>();
        LocalDate today = LocalDate.now(zoneOffset);

        long citySunrise = forecastRoot.path("city").path("sunrise").asLong(sunrise);
        long citySunset = forecastRoot.path("city").path("sunset").asLong(sunset);
        String sunriseStr = citySunrise > 0
                ? Instant.ofEpochSecond(citySunrise).atOffset(zoneOffset).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : "";
        String sunsetStr = citySunset > 0
                ? Instant.ofEpochSecond(citySunset).atOffset(zoneOffset).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : "";

        for (Map.Entry<String, List<JsonNode>> entry : dayBuckets.entrySet()) {
            String dateStr = entry.getKey();
            List<JsonNode> items = entry.getValue();

            LocalDate entryDate = LocalDate.parse(dateStr);
            String dayName;
            if (entryDate.equals(today)) {
                dayName = "Today";
            } else if (entryDate.equals(today.plusDays(1))) {
                dayName = "Tomorrow";
            } else {
                dayName = entryDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            }

            double maxTemp = -100.0;
            double minTemp = 100.0;
            double precipitationSum = 0.0;
            double maxPop = 0.0;
            double maxWindMs = 0.0;
            Map<Integer, Integer> codeCounts = new HashMap<>();

            for (JsonNode it : items) {
                double tMax = it.path("main").path("temp_max").asDouble(it.path("main").path("temp").asDouble());
                double tMin = it.path("main").path("temp_min").asDouble(it.path("main").path("temp").asDouble());
                if (tMax > maxTemp) maxTemp = tMax;
                if (tMin < minTemp) minTemp = tMin;

                precipitationSum += it.path("rain").path("3h").asDouble(0.0);
                double p = it.path("pop").asDouble(0.0);
                if (p > maxPop) maxPop = p;

                double w = it.path("wind").path("speed").asDouble(0.0);
                if (w > maxWindMs) maxWindMs = w;

                JsonNode wArr = it.path("weather");
                if (wArr.isArray() && !wArr.isEmpty()) {
                    int id = wArr.get(0).path("id").asInt(800);
                    codeCounts.put(id, codeCounts.getOrDefault(id, 0) + 1);
                }
            }

            // Find dominant weather ID for the day
            int dominantId = codeCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(800);

            WeatherConditionMapper.ConditionResult dayCond =
                    WeatherConditionMapper.mapOpenWeatherCondition(dominantId, true, "");

            int maxWindKmH = (int) Math.round(maxWindMs * 3.6);

            daily.add(new DailyForecastDto(
                    dateStr,
                    dayName,
                    (int) Math.round(maxTemp),
                    (int) Math.round(minTemp),
                    Math.round(precipitationSum * 10.0) / 10.0,
                    Math.round(precipitationSum * 10.0) / 10.0,
                    (int) Math.round(maxPop * 100),
                    maxWindKmH,
                    sunriseStr,
                    sunsetStr,
                    dayCond.weatherCode(),
                    dayCond.label()
            ));
        }

        WeatherMetadataDto metadata = new WeatherMetadataDto(
                "openweather",
                Instant.now().toString()
        );

        return new WeatherResponseDto(
                coordinates,
                location,
                timezoneStr,
                current,
                hourly,
                daily,
                metadata
        );
    }
}
