package com.weatherdashboard.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.weatherdashboard.model.WeatherData;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OpenMeteoClient {
    private static final DateTimeFormatter HOURLY_TIME = DateTimeFormatter.ISO_DATE_TIME;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WeatherData fetchWeather(String city) throws IOException, InterruptedException {
        JsonObject geo = geocodeCity(city);
        if (geo == null) {
            throw new IllegalArgumentException("City not found: " + city);
        }

        double lat = geo.get("latitude").getAsDouble();
        double lon = geo.get("longitude").getAsDouble();
        String normalizedCity = geo.get("name").getAsString();

        String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,uv_index"
                + "&hourly=temperature_2m,weather_code"
                + "&daily=temperature_2m_max,temperature_2m_min,weather_code"
                + "&timezone=auto";

        JsonObject weatherRoot = getJson(weatherUrl);
        JsonObject current = weatherRoot.getAsJsonObject("current");
        JsonObject hourly = weatherRoot.getAsJsonObject("hourly");
        JsonObject daily = weatherRoot.getAsJsonObject("daily");

        WeatherData.Current currentData = new WeatherData.Current(
                current.get("temperature_2m").getAsDouble(),
                current.get("relative_humidity_2m").getAsInt(),
                current.get("wind_speed_10m").getAsDouble(),
                current.get("uv_index").isJsonNull() ? 0.0 : current.get("uv_index").getAsDouble(),
                weatherCodeToText(current.get("weather_code").getAsInt())
        );

        List<WeatherData.HourlyForecast> hourlyForecast = new ArrayList<>();
        JsonArray hourlyTimes = hourly.getAsJsonArray("time");
        JsonArray hourlyTemp = hourly.getAsJsonArray("temperature_2m");
        JsonArray hourlyCode = hourly.getAsJsonArray("weather_code");
        int hourlyItems = Math.min(24, hourlyTimes.size());
        for (int i = 0; i < hourlyItems; i++) {
            LocalDateTime time = LocalDateTime.parse(hourlyTimes.get(i).getAsString(), HOURLY_TIME);
            hourlyForecast.add(new WeatherData.HourlyForecast(
                    time,
                    hourlyTemp.get(i).getAsDouble(),
                    weatherCodeToText(hourlyCode.get(i).getAsInt())
            ));
        }

        List<WeatherData.DailyForecast> dailyForecast = new ArrayList<>();
        JsonArray dailyDates = daily.getAsJsonArray("time");
        JsonArray dailyMax = daily.getAsJsonArray("temperature_2m_max");
        JsonArray dailyMin = daily.getAsJsonArray("temperature_2m_min");
        JsonArray dailyCode = daily.getAsJsonArray("weather_code");
        int dailyItems = Math.min(7, dailyDates.size());
        for (int i = 0; i < dailyItems; i++) {
            dailyForecast.add(new WeatherData.DailyForecast(
                    LocalDate.parse(dailyDates.get(i).getAsString()),
                    dailyMax.get(i).getAsDouble(),
                    dailyMin.get(i).getAsDouble(),
                    weatherCodeToText(dailyCode.get(i).getAsInt())
            ));
        }

        return new WeatherData(normalizedCity, lat, lon, currentData, hourlyForecast, dailyForecast);
    }

    private JsonObject geocodeCity(String city) throws IOException, InterruptedException {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1";
        JsonObject root = getJson(geoUrl);
        if (!root.has("results")) {
            return null;
        }
        JsonArray results = root.getAsJsonArray("results");
        return results.isEmpty() ? null : results.get(0).getAsJsonObject();
    }

    private JsonObject getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("API request failed with status: " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private String weatherCodeToText(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55, 56, 57 -> "Drizzle";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain";
            case 71, 73, 75, 77, 85, 86 -> "Snow";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown";
        };
    }
}
