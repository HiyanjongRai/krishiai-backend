package com.krishiai.weather.mapper;

public final class WeatherConditionMapper {

    private WeatherConditionMapper() {
    }

    public record ConditionResult(String label, int weatherCode) {
    }

    /**
     * Maps OpenWeather condition code (ID 200..804) into KrishiAI normalized condition label
     * and compatible weatherCode.
     *
     * @param weatherId   OpenWeather weather ID
     * @param isDay       Day or night flag
     * @param description Raw OpenWeather description string (optional)
     * @return Normalized ConditionResult
     */
    public static ConditionResult mapOpenWeatherCondition(int weatherId, boolean isDay, String description) {
        // Group 2xx: Thunderstorm
        if (weatherId >= 200 && weatherId < 300) {
            if (weatherId == 211 || weatherId == 210) {
                return new ConditionResult("Thunderstorm", 95);
            }
            if (weatherId >= 230) {
                return new ConditionResult("Thunderstorm with Drizzle", 95);
            }
            return new ConditionResult("Thunderstorm with Rain", 96);
        }

        // Group 3xx: Drizzle
        if (weatherId >= 300 && weatherId < 400) {
            if (weatherId == 300 || weatherId == 310) {
                return new ConditionResult("Light Drizzle", 51);
            }
            if (weatherId == 301 || weatherId == 311) {
                return new ConditionResult("Moderate Drizzle", 53);
            }
            return new ConditionResult("Dense Drizzle", 55);
        }

        // Group 5xx: Rain
        if (weatherId >= 500 && weatherId < 600) {
            if (weatherId == 500) {
                return new ConditionResult("Slight Rain", 61);
            }
            if (weatherId == 501) {
                return new ConditionResult("Moderate Rain", 63);
            }
            if (weatherId >= 502 && weatherId <= 504) {
                return new ConditionResult("Heavy Rain", 65);
            }
            if (weatherId == 511) {
                return new ConditionResult("Freezing Rain", 66);
            }
            if (weatherId == 520) {
                return new ConditionResult("Slight Showers", 80);
            }
            if (weatherId == 521) {
                return new ConditionResult("Moderate Showers", 81);
            }
            if (weatherId >= 522) {
                return new ConditionResult("Violent Showers", 82);
            }
            return new ConditionResult("Moderate Rain", 63);
        }

        // Group 6xx: Snow
        if (weatherId >= 600 && weatherId < 700) {
            if (weatherId == 600) {
                return new ConditionResult("Slight Snow", 71);
            }
            if (weatherId == 601) {
                return new ConditionResult("Moderate Snow", 73);
            }
            if (weatherId == 602) {
                return new ConditionResult("Heavy Snow", 75);
            }
            if (weatherId >= 611 && weatherId <= 616) {
                return new ConditionResult("Snow Grains", 77);
            }
            return new ConditionResult("Snow Showers", 85);
        }

        // Group 7xx: Atmosphere
        if (weatherId >= 700 && weatherId < 800) {
            if (weatherId == 741) {
                return new ConditionResult("Fog", 45);
            }
            if (weatherId == 721) {
                return new ConditionResult("Haze", 45);
            }
            if (weatherId == 711) {
                return new ConditionResult("Smoke", 45);
            }
            if (weatherId == 781) {
                return new ConditionResult("Tornado", 95);
            }
            return new ConditionResult("Foggy", 45);
        }

        // Group 800: Clear
        if (weatherId == 800) {
            return new ConditionResult(isDay ? "Clear Sky" : "Clear Night", 0);
        }

        // Group 80x: Clouds
        if (weatherId == 801) {
            return new ConditionResult("Mainly Clear", 1);
        }
        if (weatherId == 802) {
            return new ConditionResult("Partly Cloudy", 2);
        }
        if (weatherId == 803 || weatherId == 804) {
            return new ConditionResult("Overcast", 3);
        }

        // Fallback using description if available
        if (description != null && !description.isBlank()) {
            String capitalized = description.substring(0, 1).toUpperCase() + description.substring(1);
            return new ConditionResult(capitalized, isDay ? 1 : 2);
        }

        return new ConditionResult("Fair", 1);
    }
}
