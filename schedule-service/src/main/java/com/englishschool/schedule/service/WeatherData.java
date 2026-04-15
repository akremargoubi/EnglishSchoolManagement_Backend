package com.englishschool.schedule.service;

public record WeatherData(
        double temperature,
        double minTemperature,
        int rainProbability,
        int weatherCode,
        String condition
) {
    public WeatherData() {
        this(0, 0, 0, 0, "");
    }
}
