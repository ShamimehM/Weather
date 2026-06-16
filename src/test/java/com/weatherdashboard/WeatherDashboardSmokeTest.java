package com.weatherdashboard;

import com.weatherdashboard.model.WeatherData;
import com.weatherdashboard.service.OpenMeteoClient;
import com.weatherdashboard.storage.LocalStorage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Headless smoke test — run with: mvn test-compile exec:java@smoke-test
 */
public class WeatherDashboardSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Weather Dashboard Smoke Test ===\n");
        Path tempDir = Files.createTempDirectory("weather-dashboard-test-");

        try {
            testWeatherApi();
            testLocalStorage(tempDir);
            testClothingLogic();
        } finally {
            deleteRecursively(tempDir);
        }

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testWeatherApi() throws Exception {
        System.out.println("[API] Fetching Oslo...");
        OpenMeteoClient client = new OpenMeteoClient();
        WeatherData oslo = client.fetchWeather("Oslo");

        assertTrue(oslo.city() != null && !oslo.city().isBlank(), "Oslo city name present");
        assertTrue(oslo.current().temperatureC() > -50 && oslo.current().temperatureC() < 60, "Oslo temp in range");
        assertTrue(oslo.current().humidity() >= 0 && oslo.current().humidity() <= 100, "Oslo humidity 0-100");
        assertTrue(oslo.hourlyForecast().size() == 24, "24 hourly entries");
        assertTrue(oslo.dailyForecast().size() == 7, "7 daily entries");
        System.out.printf("  Oslo: %.1f C, %s, humidity %d%%%n",
                oslo.current().temperatureC(), oslo.current().conditions(), oslo.current().humidity());

        System.out.println("[API] Fetching Tehran...");
        WeatherData tehran = client.fetchWeather("Tehran");
        assertTrue(tehran.city().toLowerCase().contains("tehran") || tehran.city().toLowerCase().contains("teheran"),
                "Tehran geocoded");
        System.out.printf("  Tehran: %.1f C, %s%n", tehran.current().temperatureC(), tehran.current().conditions());

        System.out.println("[API] Invalid city should fail...");
        try {
            client.fetchWeather("ZZZNOTACITY12345");
            fail("Expected exception for invalid city");
        } catch (IllegalArgumentException expected) {
            pass("Invalid city rejected");
        }
    }

    private static void testLocalStorage(Path workDir) throws Exception {
        System.out.println("[Storage] Favorites and history...");
        LocalStorage storage = new LocalStorage(workDir.resolve(".weather-dashboard"));

        storage.addFavorite("Oslo");
        storage.addFavorite("London");
        storage.addFavorite("Oslo");

        List<String> favorites = storage.loadFavorites();
        assertTrue(favorites.size() == 2, "Two unique favorites");
        assertTrue(favorites.contains("Oslo") && favorites.contains("London"), "Favorites contain cities");

        storage.saveSearchHistory("Oslo");
        storage.saveSearchHistory("Tokyo");
        List<String> history = storage.loadSearchHistory();
        assertTrue(history.size() >= 2, "History has entries");
        assertTrue(history.get(history.size() - 1).contains("Tokyo"), "Last history is Tokyo");

        storage.removeFavorite("London");
        assertTrue(storage.loadFavorites().size() == 1, "One favorite after remove");
        pass("Local storage OK");
    }

    private static void testClothingLogic() {
        System.out.println("[Logic] Clothing recommendations...");
        String coldRain = recommend(10, "Rain");
        assertTrue(coldRain.contains("Jacket") && coldRain.contains("Umbrella"), "Cold rain -> jacket + umbrella");

        String hotClear = recommend(28, "Clear sky");
        assertTrue(hotClear.contains("Shorts") && !hotClear.contains("Jacket"), "Hot clear -> shorts");

        String mild = recommend(18, "Partly cloudy");
        assertTrue(mild.contains("Regular outfit"), "Mild -> regular outfit");
        pass("Clothing logic OK");
    }

    static String recommend(double temperature, String condition) {
        String conditionLower = condition == null ? "" : condition.toLowerCase();
        boolean rainy = conditionLower.contains("rain") || conditionLower.contains("drizzle");
        boolean cold = temperature < 15;
        boolean hot = temperature >= 24;

        StringBuilder sb = new StringBuilder();
        if (cold) sb.append("Jacket");
        if (rainy) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Umbrella");
        }
        if (hot) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("Shorts");
        }
        if (sb.isEmpty()) sb.append("Regular outfit");
        return sb.toString();
    }

    private static void assertTrue(boolean condition, String message) {
        if (condition) {
            pass(message);
        } else {
            fail(message);
        }
    }

    private static void pass(String message) {
        passed++;
        System.out.println("  PASS: " + message);
    }

    private static void fail(String message) {
        failed++;
        System.out.println("  FAIL: " + message);
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
