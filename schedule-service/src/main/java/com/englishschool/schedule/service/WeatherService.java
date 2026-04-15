package com.englishschool.schedule.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, WeatherData> cache = new HashMap<>();

    // Free API - OpenMeteo (no API key required!)
    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast";

    public WeatherData getWeatherForDay(String dayOfWeek, LocalDate date) {
        String cacheKey = date.toString();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        // Coordinates for Tunis, Tunisia (change to your city)
        String url = String.format("%s?latitude=36.8065&longitude=10.1815&daily=temperature_2m_max,temperature_2m_min,precipitation_probability,weathercode&timezone=auto&start_date=%s&end_date=%s",
                WEATHER_API, date, date);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> daily = (Map<String, Object>) response.get("daily");

            WeatherData weather = new WeatherData();
            if (daily != null) {
                @SuppressWarnings("unchecked")
                List<Number> temps = (List<Number>) daily.get("temperature_2m_max");
                @SuppressWarnings("unchecked")
                List<Number> minTemps = (List<Number>) daily.get("temperature_2m_min");
                @SuppressWarnings("unchecked")
                List<Integer> rainProbs = (List<Integer>) daily.get("precipitation_probability");
                @SuppressWarnings("unchecked")
                List<Integer> codes = (List<Integer>) daily.get("weathercode");

                double temperature = temps != null && !temps.isEmpty() ? temps.get(0).doubleValue() : 22.0;
                double minTemperature = minTemps != null && !minTemps.isEmpty() ? minTemps.get(0).doubleValue() : 18.0;
                int rainProbability = rainProbs != null && !rainProbs.isEmpty() ? rainProbs.get(0) : 10;
                int weatherCode = codes != null && !codes.isEmpty() ? codes.get(0) : 1;
                String condition = getWeatherCondition(weatherCode);

                weather = new WeatherData(temperature, minTemperature, rainProbability, weatherCode, condition);
            }

            cache.put(cacheKey, weather);
            return weather;
        } catch (Exception e) {
            // Return fallback data
            return new WeatherData(22.0, 18.0, 10, 1, "⛅ Partly Cloudy");
        }
    }

    private String getWeatherCondition(int code) {
        if (code == 0) return "☀️ Clear Sky";
        if (code <= 3) return "⛅ Partly Cloudy";
        if (code <= 49) return "🌫️ Foggy";
        if (code <= 59) return "🌧️ Light Rain";
        if (code <= 69) return "🌧️ Rainy";
        if (code <= 79) return "❄️ Snow";
        if (code <= 99) return "⛈️ Thunderstorm";
        return "🌡️ Unknown";
    }
}