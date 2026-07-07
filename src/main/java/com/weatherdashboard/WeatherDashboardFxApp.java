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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
    private final Label statusLabel = new Label("Search for a city to get started.");
    private final Label cityLabel = new Label("—");
    private final Label tempLabel = new Label("—");
    private final Label conditionsLabel = new Label("No data yet");
    private final Label humidityLabel = new Label("—");
    private final Label windLabel = new Label("—");
    private final Label uvLabel = new Label("—");
    private final Label clothingLabel = new Label("—");
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();

    private final ObservableList<String> hourlyItems = FXCollections.observableArrayList();
    private final ObservableList<String> dailyItems = FXCollections.observableArrayList();
    private final ObservableList<String> favoritesItems = FXCollections.observableArrayList();
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) throws Exception {
        storage = new LocalStorage();
        loadLocalLists();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setPadding(new Insets(24));

        root.setTop(buildHeader());
        root.setCenter(buildMainContent());
        root.setRight(buildSidebar());
        root.setBottom(statusLabel);

        BorderPane.setMargin(root.getCenter(), new Insets(20, 20, 0, 0));
        BorderPane.setMargin(root.getRight(), new Insets(20, 0, 0, 0));

        statusLabel.getStyleClass().add("status-bar");

        Scene scene = new Scene(root, 1200, 720);
        scene.getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        stage.setTitle("Weather Dashboard");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildHeader() {
        Label title = new Label("Weather");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Live forecasts powered by Open-Meteo");
        subtitle.getStyleClass().add("app-subtitle");

        cityField.setPromptText("Enter a city…");
        cityField.setPrefWidth(220);
        cityField.setOnAction(event -> searchWeather());

        cityQuickSelect.getItems().setAll(DEFAULT_CITIES);
        cityQuickSelect.setPromptText("Quick pick");
        cityQuickSelect.setOnAction(event -> {
            String selected = cityQuickSelect.getValue();
            if (selected != null) {
                cityField.setText(selected);
                searchWeather();
            }
        });

        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(event -> searchWeather());

        Button saveFavoriteBtn = new Button("★ Save");
        saveFavoriteBtn.getStyleClass().add("button-secondary");
        saveFavoriteBtn.setOnAction(event -> saveCurrentAsFavorite());

        HBox searchRow = new HBox(10, cityField, cityQuickSelect, searchBtn, saveFavoriteBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("search-bar");

        VBox header = new VBox(6, title, subtitle, searchRow);
        return header;
    }

    private VBox buildMainContent() {
        VBox hero = buildHeroCard();

        ListView<String> hourlyList = styledList(hourlyItems, "Hourly data will appear here.");
        VBox hourlyCard = card("Next 24 Hours", hourlyList);
        VBox.setVgrow(hourlyList, Priority.ALWAYS);

        ListView<String> dailyList = styledList(dailyItems, "7-day forecast will appear here.");
        VBox dailyCard = card("7-Day Outlook", dailyList);
        VBox.setVgrow(dailyList, Priority.ALWAYS);

        HBox forecasts = new HBox(16, hourlyCard, dailyCard);
        HBox.setHgrow(hourlyCard, Priority.ALWAYS);
        HBox.setHgrow(dailyCard, Priority.ALWAYS);
        hourlyCard.setMaxWidth(Double.MAX_VALUE);
        dailyCard.setMaxWidth(Double.MAX_VALUE);

        VBox main = new VBox(16, hero, forecasts);
        VBox.setVgrow(forecasts, Priority.ALWAYS);
        return main;
    }

    private VBox buildHeroCard() {
        cityLabel.getStyleClass().add("city-name");
        tempLabel.getStyleClass().add("temp-hero");
        conditionsLabel.getStyleClass().add("conditions-hero");
        clothingLabel.getStyleClass().add("clothing-badge");

        loadingIndicator.setMaxSize(32, 32);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);

        VBox tempBlock = new VBox(4, cityLabel, tempLabel, conditionsLabel, clothingLabel);
        tempBlock.setAlignment(Pos.CENTER_LEFT);

        HBox heroTop = new HBox(16, tempBlock, loadingIndicator);
        heroTop.setAlignment(Pos.CENTER_LEFT);

        GridPane stats = new GridPane();
        stats.setHgap(24);
        stats.setVgap(12);
        stats.add(statBlock("Humidity", humidityLabel), 0, 0);
        stats.add(statBlock("Wind", windLabel), 1, 0);
        stats.add(statBlock("UV Index", uvLabel), 2, 0);

        VBox hero = new VBox(20, heroTop, stats);
        hero.getStyleClass().add("card-hero");
        return hero;
    }

    private VBox statBlock(String label, Label value) {
        Label name = new Label(label);
        name.getStyleClass().add("stat-label");
        value.getStyleClass().add("stat-value");
        return new VBox(4, name, value);
    }

    private VBox buildSidebar() {
        ListView<String> favoritesList = styledList(favoritesItems, "No saved cities.");
        favoritesList.setOnMouseClicked(event -> {
            String selected = favoritesList.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() == 2) {
                cityField.setText(selected);
                searchWeather();
            }
        });

        Button removeFavoriteBtn = new Button("Remove");
        removeFavoriteBtn.getStyleClass().addAll("button-danger");
        removeFavoriteBtn.setOnAction(event -> removeSelectedFavorite(favoritesList));

        VBox favoritesCard = card("Favorites", favoritesList, removeFavoriteBtn);
        VBox.setVgrow(favoritesList, Priority.ALWAYS);
        favoritesCard.setPrefWidth(260);
        favoritesCard.setMaxWidth(260);

        ListView<String> historyList = styledList(historyItems, "No search history.");
        VBox historyCard = card("Recent Searches", historyList);
        VBox.setVgrow(historyList, Priority.ALWAYS);
        historyCard.setPrefWidth(260);
        historyCard.setMaxWidth(260);

        VBox sidebar = new VBox(16, favoritesCard, historyCard);
        VBox.setVgrow(favoritesCard, Priority.ALWAYS);
        VBox.setVgrow(historyCard, Priority.ALWAYS);
        return sidebar;
    }

    private ListView<String> styledList(ObservableList<String> items, String placeholder) {
        ListView<String> list = new ListView<>(items);
        list.setPlaceholder(new Label(placeholder));
        list.setPrefHeight(200);
        return list;
    }

    private VBox card(String title, javafx.scene.Node... nodes) {
        Label header = new Label(title.toUpperCase());
        header.getStyleClass().add("card-title");

        VBox box = new VBox(12, header);
        box.getChildren().addAll(nodes);
        box.getStyleClass().add("card");
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
            setStatus("Enter a city name to search.");
            return;
        }

        setLoading(true);
        setStatus("Loading weather for " + city + "…");

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
                    setLoading(false);
                    setStatus("Updated · " + data.city());
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        setStatus("Couldn't load weather: " + rootMessage(ex));
                    });
                    return null;
                });
    }

    private void renderWeather(WeatherData data) {
        cityLabel.setText(data.city());
        tempLabel.setText(String.format("%.0f°", data.current().temperatureC()));
        conditionsLabel.setText(data.current().conditions());
        humidityLabel.setText(data.current().humidity() + "%");
        windLabel.setText(String.format("%.0f km/h", data.current().windSpeedKmh()));
        uvLabel.setText(String.format("%.1f", data.current().uvIndex()));
        clothingLabel.setText(clothingRecommendation(data.current().temperatureC(), data.current().conditions()));

        hourlyItems.clear();
        for (WeatherData.HourlyForecast hourly : data.hourlyForecast()) {
            hourlyItems.add(String.format("%-5s   %.0f°   %s",
                    hourly.time().toLocalTime().toString().substring(0, 5),
                    hourly.temperatureC(),
                    hourly.conditions()));
        }

        dailyItems.clear();
        for (WeatherData.DailyForecast daily : data.dailyForecast()) {
            dailyItems.add(String.format("%s   %.0f° / %.0f°   %s",
                    daily.date(),
                    daily.maxTempC(),
                    daily.minTempC(),
                    daily.conditions()));
        }
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
    }

    private void saveCurrentAsFavorite() {
        String city = cityField.getText() == null ? "" : cityField.getText().trim();
        if (city.isBlank()) {
            setStatus("Search for a city first.");
            return;
        }
        try {
            storage.addFavorite(city);
            refreshFavorites();
            setStatus("Saved " + city + " to favorites.");
        } catch (IOException e) {
            setStatus("Couldn't save favorite: " + e.getMessage());
        }
    }

    private void removeSelectedFavorite(ListView<String> favoritesList) {
        String selected = favoritesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a favorite to remove.");
            return;
        }
        try {
            storage.removeFavorite(selected);
            refreshFavorites();
            setStatus("Removed " + selected + ".");
        } catch (IOException e) {
            setStatus("Couldn't remove favorite: " + e.getMessage());
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
            setStatus("Couldn't read favorites.");
        }
    }

    private void refreshHistory() {
        try {
            List<String> raw = storage.loadSearchHistory();
            List<String> display = raw.stream()
                    .map(entry -> {
                        int sep = entry.lastIndexOf(" | ");
                        return sep >= 0 ? entry.substring(sep + 3) : entry;
                    })
                    .toList();
            historyItems.setAll(display);
        } catch (IOException e) {
            setStatus("Couldn't read history.");
        }
    }

    private String clothingRecommendation(double temperature, String condition) {
        String conditionLower = condition == null ? "" : condition.toLowerCase();
        boolean rainy = conditionLower.contains("rain") || conditionLower.contains("drizzle");
        boolean cold = temperature < 15;
        boolean hot = temperature >= 24;

        StringBuilder sb = new StringBuilder();
        if (cold) sb.append("Jacket");
        if (rainy) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append("Umbrella");
        }
        if (hot) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append("Shorts");
        }
        if (sb.isEmpty()) sb.append("Light layers");
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
