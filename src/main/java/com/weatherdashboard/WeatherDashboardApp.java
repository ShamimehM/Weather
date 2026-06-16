package com.weatherdashboard;

import com.weatherdashboard.model.WeatherData;
import com.weatherdashboard.service.OpenMeteoClient;
import com.weatherdashboard.storage.LocalStorage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class WeatherDashboardApp {
    private static final List<String> DEFAULT_CITIES = List.of(
            "Oslo", "Rasht", "Tehran", "Shiraz", "Tabriz", "London", "Tokyo", "Toronto", "Sydney"
    );
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("EEE HH:mm");

    public static void main(String[] args) {
        try {
            new WeatherDashboardApp().run();
        } catch (Exception ex) {
            System.out.println("Application failed: " + ex.getMessage());
        }
    }

    private final OpenMeteoClient weatherClient = new OpenMeteoClient();
    private final Scanner scanner = new Scanner(System.in);
    private LocalStorage storage;

    private void run() throws IOException {
        storage = new LocalStorage();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> searchWeather();
                case "2" -> showFavorites();
                case "3" -> addFavorite();
                case "4" -> removeFavorite();
                case "5" -> showHistory();
                case "6" -> showPlannedAdvancedFeatures();
                case "0" -> running = false;
                default -> System.out.println("Invalid option.\n");
            }
        }
        System.out.println("Bye!");
    }

    private void printMenu() {
        System.out.println("""
                ==============================
                Weather Dashboard (Java)
                ==============================
                1) Search weather
                2) View favorites
                3) Save city as favorite
                4) Remove favorite city
                5) View weather history
                6) Advanced features roadmap
                0) Exit
                Choose:\
                """);
    }

    private void searchWeather() {
        try {
            System.out.println("Default cities: " + String.join(", ", DEFAULT_CITIES));
            System.out.print("Enter city: ");
            String city = scanner.nextLine().trim();
            if (city.isBlank()) {
                System.out.println("City cannot be empty.\n");
                return;
            }

            WeatherData data = weatherClient.fetchWeather(city);
            storage.saveSearchHistory(data.city());
            printWeather(data);
            printClothingRecommendation(data.current().temperatureC(), data.current().conditions());
            System.out.println();
        } catch (Exception ex) {
            System.out.println("Failed to fetch weather: " + ex.getMessage() + "\n");
        }
    }

    private void printWeather(WeatherData data) {
        System.out.println("\n--- Current Weather for " + data.city() + " ---");
        System.out.printf("Temperature: %.1f°C%n", data.current().temperatureC());
        System.out.printf("Humidity: %d%%%n", data.current().humidity());
        System.out.printf("Wind: %.1f km/h%n", data.current().windSpeedKmh());
        System.out.printf("UV Index: %.1f%n", data.current().uvIndex());
        System.out.println("Conditions: " + data.current().conditions());

        System.out.println("\n--- Hourly Forecast (24h) ---");
        for (WeatherData.HourlyForecast hourly : data.hourlyForecast()) {
            System.out.printf("%s | %.1f°C | %s%n",
                    hourly.time().format(HOUR_FORMAT),
                    hourly.temperatureC(),
                    hourly.conditions());
        }

        System.out.println("\n--- 7-Day Forecast ---");
        for (WeatherData.DailyForecast daily : data.dailyForecast()) {
            System.out.printf("%s | max %.1f°C / min %.1f°C | %s%n",
                    daily.date(),
                    daily.maxTempC(),
                    daily.minTempC(),
                    daily.conditions());
        }
    }

    private void showFavorites() {
        try {
            List<String> favorites = storage.loadFavorites();
            if (favorites.isEmpty()) {
                System.out.println("No favorite cities yet.\n");
                return;
            }
            System.out.println("--- Favorite Cities ---");
            favorites.forEach(city -> System.out.println("- " + city));
            System.out.println();
        } catch (IOException ex) {
            System.out.println("Cannot read favorites: " + ex.getMessage() + "\n");
        }
    }

    private void addFavorite() {
        try {
            System.out.print("City to save: ");
            String city = scanner.nextLine().trim();
            if (city.isBlank()) {
                System.out.println("City cannot be empty.\n");
                return;
            }
            storage.addFavorite(city);
            System.out.println("Saved to favorites.\n");
        } catch (IOException ex) {
            System.out.println("Cannot save favorite: " + ex.getMessage() + "\n");
        }
    }

    private void removeFavorite() {
        try {
            System.out.print("City to remove: ");
            String city = scanner.nextLine().trim();
            if (city.isBlank()) {
                System.out.println("City cannot be empty.\n");
                return;
            }
            storage.removeFavorite(city);
            System.out.println("Removed from favorites.\n");
        } catch (IOException ex) {
            System.out.println("Cannot remove favorite: " + ex.getMessage() + "\n");
        }
    }

    private void showHistory() {
        try {
            List<String> history = storage.loadSearchHistory();
            if (history.isEmpty()) {
                System.out.println("No weather history yet.\n");
                return;
            }
            System.out.println("--- Weather History ---");
            history.forEach(item -> System.out.println("- " + item));
            System.out.println();
        } catch (IOException ex) {
            System.out.println("Cannot read history: " + ex.getMessage() + "\n");
        }
    }

    private void showPlannedAdvancedFeatures() {
        System.out.println("""
                --- Planned Advanced Features ---
                - Weather alerts
                - Air quality
                - Sunrise/sunset
                - Clothing recommendations (already basic)
                """);
        System.out.println();
    }

    private void printClothingRecommendation(double temperature, String condition) {
        System.out.println("\n--- Clothing Recommendation ---");
        System.out.printf("Example: %.1f°C and %s%n", temperature, condition.toLowerCase());

        boolean rainy = condition.toLowerCase().contains("rain") || condition.toLowerCase().contains("drizzle");
        boolean cold = temperature < 15;
        boolean hot = temperature >= 24;

        System.out.println(cold ? "✓ Jacket" : "✗ Jacket");
        System.out.println(rainy ? "✓ Umbrella" : "✗ Umbrella");
        System.out.println(hot ? "✓ Shorts" : "✗ Shorts");
    }
}
