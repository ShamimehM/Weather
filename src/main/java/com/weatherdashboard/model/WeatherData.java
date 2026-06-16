package com.weatherdashboard.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WeatherData(
        String city,
        double latitude,
        double longitude,
        Current current,
        List<HourlyForecast> hourlyForecast,
        List<DailyForecast> dailyForecast
) {
    public record Current(
            double temperatureC,
            int humidity,
            double windSpeedKmh,
            double uvIndex,
            String conditions
    ) {}

    public record HourlyForecast(
            LocalDateTime time,
            double temperatureC,
            String conditions
    ) {}

    public record DailyForecast(
            LocalDate date,
            double maxTempC,
            double minTempC,
            String conditions
    ) {}
}
