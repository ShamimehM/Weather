package com.weatherdashboard;

import com.weatherdashboard.model.WeatherData;
import com.weatherdashboard.service.OpenMeteoClient;
import com.weatherdashboard.storage.LocalStorage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WeatherDashboardFxApp extends Application {
    private static final List<String> DEFAULT_CITIES = List.of(
            "Oslo", "Rasht", "Tehran", "Shiraz", "Tabriz", "London", "Tokyo", "Toronto", "Sydney"
    );

    private final OpenMeteoClient weatherClient = new OpenMeteoClient();
    private LocalStorage storage;

    private final TextField cityField = new TextField();
    private final ComboBox<String> cityQuickSelect = new ComboBox<>();
    private final Label statusLabel = new Label("Ready.");
    private final Label locationLabel = new Label("Search for a city to load weather.");

    private final Label tempLabel = new Label("-");
    private final Label humidityLabel = new Label("-");
    private final Label windLabel = new Label("-");
    private final Label uvLabel = new Label("-");
    private final Label conditionsLabel = new Label("-");
    private final Label clothingLabel = new Label("-");

    private final ObservableList<String> hourlyItems = FXCollections.observableArrayList();
    private final ObservableList<String> dailyItems = FXCollections.observableArrayList();
    private final ObservableList<String> favoritesItems = FXCollections.observableArrayList();
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) throws Exception {
        storage = new LocalStorage();
        loadLocalLists();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        VBox top = buildTopBar();
        HBox center = buildCenterPanels();
        VBox right = buildRightPanels();

        root.setTop(top);
        root.setCenter(center);
        root.setRight(right);
        root.setBottom(statusLabel);

        BorderPane.setMargin(center, new Insets(12, 12, 0, 0));
        BorderPane.setMargin(right, new Insets(12, 0, 0, 12));
        BorderPane.setMargin(statusLabel, new Insets(10, 0, 0, 0));

        Scene scene = new Scene(root, 1280, 760);
        stage.setTitle("Weather Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildTopBar() {
        Label title = new Label("Weather Dashboard");
        title.setFont(Font.font(24));

        cityField.setPromptText("Enter city name");
        cityField.setPrefWidth(240);
        cityField.setOnAction(event -> searchWeather());

        cityQuickSelect.getItems().setAll(DEFAULT_CITIES);
        cityQuickSelect.setPromptText("Quick cities");
        cityQuickSelect.setOnAction(event -> {
            String selected = cityQuickSelect.getValue();
            if (selected != null) {
                cityField.setText(selected);
            }
        });

        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(event -> searchWeather());

        Button saveFavoriteBtn = new Button("Add Favorite");
        saveFavoriteBtn.setOnAction(event -> saveCurrentAsFavorite());
        saveFavoriteBtn.setTooltip(new Tooltip("Save the city in the search box"));

        HBox controls = new HBox(8, cityField, cityQuickSelect, searchBtn, saveFavoriteBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        locationLabel.setFont(Font.font(16));
        VBox top = new VBox(8, title, controls, locationLabel);
        return top;
    }

    private HBox buildCenterPanels() {
        VBox currentCard = buildCurrentCard();

        ListView<String> hourlyList = new ListView<>(hourlyItems);
        hourlyList.setPlaceholder(new Label("No hourly forecast yet."));
        VBox hourlyBox = panel("Hourly Forecast (24h)", hourlyList);

        ListView<String> dailyList = new ListView<>(dailyItems);
        dailyList.setPlaceholder(new Label("No 7-day forecast yet."));
        VBox dailyBox = panel("7-Day Forecast", dailyList);

        HBox center = new HBox(12, currentCard, hourlyBox, dailyBox);
        HBox.setHgrow(currentCard, Priority.ALWAYS);
        HBox.setHgrow(hourlyBox, Priority.ALWAYS);
        HBox.setHgrow(dailyBox, Priority.ALWAYS);
        currentCard.setPrefWidth(320);
        hourlyBox.setPrefWidth(360);
        dailyBox.setPrefWidth(360);
        return center;
    }

    private VBox buildCurrentCard() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Temperature"), tempLabel);
        grid.addRow(1, new Label("Humidity"), humidityLabel);
        grid.addRow(2, new Label("Wind"), windLabel);
        grid.addRow(3, new Label("UV Index"), uvLabel);
        grid.addRow(4, new Label("Conditions"), conditionsLabel);
        grid.addRow(5, new Label("Wear"), clothingLabel);

        return panel("Current Weather", grid);
    }

    private VBox buildRightPanels() {
        ListView<String> favoritesList = new ListView<>(favoritesItems);
        favoritesList.setPlaceholder(new Label("No favorites saved."));
        favoritesList.setOnMouseClicked(event -> {
            String selected = favoritesList.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                cityField.setText(selected);
                searchWeather();
            }
        });

        Button removeFavoriteBtn = new Button("Remove Favorite");
        removeFavoriteBtn.setOnAction(event -> removeSelectedFavorite(favoritesList));
        VBox favoritesBox = panel("Favorites", favoritesList, removeFavoriteBtn);
        favoritesBox.setPrefWidth(280);

        ListView<String> historyList = new ListView<>(historyItems);
        historyList.setPlaceholder(new Label("No history yet."));
        VBox historyBox = panel("Weather History", historyList);
        historyBox.setPrefWidth(280);
        VBox.setVgrow(historyList, Priority.ALWAYS);

        VBox right = new VBox(12, favoritesBox, historyBox);
        VBox.setVgrow(favoritesBox, Priority.ALWAYS);
        VBox.setVgrow(historyBox, Priority.ALWAYS);
        return right;
    }

    private VBox panel(String title, javafx.scene.Node... nodes) {
        Label header = new Label(title);
        header.setFont(Font.font(16));
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.getChildren().add(header);
        box.getChildren().addAll(nodes);
        box.setStyle("-fx-background-color: #f6f8fa; -fx-border-color: #d0d7de; -fx-border-radius: 8; -fx-background-radius: 8;");
        for (javafx.scene.Node node : nodes) {
            if (node instanceof ListView<?> listView) {
                VBox.setVgrow(listView, Priority.ALWAYS);
            }
        }
        return box;
    }

    private void searchWeather() {
        String city = cityField.getText() == null ? "" : cityField.getText().trim();
        if (city.isBlank()) {
            setStatus("Please enter a city.");
            return;
        }

        setStatus("Loading weather for " + city + "...");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        WeatherData data = weatherClient.fetchWeather(city);
                        storage.saveSearchHistory(data.city());
                        return data;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(data -> Platform.runLater(() -> {
                    renderWeather(data);
                    refreshHistory();
                    setStatus("Updated weather for " + data.city() + ".");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> setStatus("Failed: " + rootMessage(ex)));
                    return null;
                });
    }

    private void renderWeather(WeatherData data) {
        locationLabel.setText(data.city() + " (" + data.latitude() + ", " + data.longitude() + ")");
        tempLabel.setText(String.format("%.1f C", data.current().temperatureC()));
        humidityLabel.setText(data.current().humidity() + " %");
        windLabel.setText(String.format("%.1f km/h", data.current().windSpeedKmh()));
        uvLabel.setText(String.format("%.1f", data.current().uvIndex()));
        conditionsLabel.setText(data.current().conditions());
        clothingLabel.setText(clothingRecommendation(data.current().temperatureC(), data.current().conditions()));

        hourlyItems.clear();
        for (WeatherData.HourlyForecast hourly : data.hourlyForecast()) {
            hourlyItems.add(String.format("%s | %.1f C | %s", hourly.time().toLocalTime(), hourly.temperatureC(), hourly.conditions()));
        }

        dailyItems.clear();
        for (WeatherData.DailyForecast daily : data.dailyForecast()) {
            dailyItems.add(String.format("%s | max %.1f C / min %.1f C | %s",
                    daily.date(), daily.maxTempC(), daily.minTempC(), daily.conditions()));
        }
    }

    private void saveCurrentAsFavorite() {
        String city = cityField.getText() == null ? "" : cityField.getText().trim();
        if (city.isBlank()) {
            setStatus("Type a city first.");
            return;
        }
        try {
            storage.addFavorite(city);
            refreshFavorites();
            setStatus("Saved " + city + " to favorites.");
        } catch (IOException e) {
            setStatus("Failed to save favorite: " + e.getMessage());
        }
    }

    private void removeSelectedFavorite(ListView<String> favoritesList) {
        String selected = favoritesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Choose a favorite to remove.");
            return;
        }
        try {
            storage.removeFavorite(selected);
            refreshFavorites();
            setStatus("Removed " + selected + " from favorites.");
        } catch (IOException e) {
            setStatus("Failed to remove favorite: " + e.getMessage());
        }
    }

    private void loadLocalLists() {
        refreshFavorites();
        refreshHistory();
    }

    private void refreshFavorites() {
        try {
            favoritesItems.setAll(storage.loadFavorites());
        } catch (IOException e) {
            setStatus("Failed to read favorites: " + e.getMessage());
        }
    }

    private void refreshHistory() {
        try {
            historyItems.setAll(storage.loadSearchHistory());
        } catch (IOException e) {
            setStatus("Failed to read history: " + e.getMessage());
        }
    }

    private String clothingRecommendation(double temperature, String condition) {
        String conditionLower = condition == null ? "" : condition.toLowerCase();
        boolean rainy = conditionLower.contains("rain") || conditionLower.contains("drizzle");
        boolean cold = temperature < 15;
        boolean hot = temperature >= 24;

        StringBuilder sb = new StringBuilder();
        if (cold) {
            sb.append("Jacket");
        }
        if (rainy) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("Umbrella");
        }
        if (hot) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("Shorts");
        }
        if (sb.isEmpty()) {
            sb.append("Regular outfit");
        }
        return sb.toString();
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.toString() : cursor.getMessage();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
