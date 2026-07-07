package com.weatherdashboard;

import com.weatherdashboard.model.SavedLocation;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WeatherDashboardFxApp extends Application {
    private static final int MAX_COMPARE = 3;
    private static final List<String> DEFAULT_CITIES = List.of(
            "Oslo", "Rasht", "Tehran", "Shiraz", "Tabriz", "London", "Tokyo", "Toronto", "Sydney"
    );

    private final OpenMeteoClient weatherClient = new OpenMeteoClient();
    private LocalStorage storage;

    private final TextField cityField = new TextField();
    private final ComboBox<String> cityQuickSelect = new ComboBox<>();
    private final Label statusLabel = new Label("Search for a city to get started.");
    private final Label cityLabel = new Label("—");
    private final Label regionLabel = new Label("");
    private final Label tempLabel = new Label("—");
    private final Label conditionsLabel = new Label("No data yet");
    private final Label humidityLabel = new Label("—");
    private final Label windLabel = new Label("—");
    private final Label uvLabel = new Label("—");
    private final Label clothingLabel = new Label("—");
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final VBox hourlyContainer = new VBox(2);
    private final VBox dailyContainer = new VBox(2);
    private final HBox compareRow = new HBox(12);
    private final VBox compareSection = new VBox(8);

    private final ObservableList<SavedLocation> favoritesItems = FXCollections.observableArrayList();
    private final ObservableList<SavedLocation> historyItems = FXCollections.observableArrayList();
    private final List<WeatherData> compareList = new ArrayList<>();

    private WeatherData currentWeather;
    private SavedLocation currentLocation;

    @Override
    public void start(Stage stage) throws Exception {
        storage = new LocalStorage();
        loadLocalLists();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setPadding(new Insets(20));

        root.setTop(buildHeader());
        root.setCenter(buildMainContent());
        root.setRight(buildSidebar());
        root.setBottom(new VBox(8, compareSection, statusLabel));

        BorderPane.setMargin(root.getCenter(), new Insets(16, 16, 0, 0));
        BorderPane.setMargin(root.getRight(), new Insets(16, 0, 0, 0));

        statusLabel.getStyleClass().add("status-bar");
        compareSection.setVisible(false);
        compareSection.setManaged(false);

        Scene scene = new Scene(root, 1280, 760);
        scene.getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm()
        );
        stage.setTitle("Weather Dashboard");
        stage.getIcons().setAll(
                AppIcon.sun(256),
                AppIcon.sun(128),
                AppIcon.sun(64),
                AppIcon.sun(48),
                AppIcon.sun(32),
                AppIcon.sun(16)
        );
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildHeader() {
        Label title = new Label("Weather Dashboard");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Live forecasts · Open-Meteo");
        subtitle.getStyleClass().add("app-subtitle");

        ImageView logo = new ImageView(AppIcon.sun(48));
        logo.setFitWidth(48);
        logo.setFitHeight(48);
        logo.setPreserveRatio(true);
        logo.getStyleClass().add("app-logo");

        VBox titleBlock = new VBox(4, title, subtitle);
        HBox topRow = new HBox(titleBlock, logo);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        topRow.setFillHeight(true);

        cityField.setPromptText("Enter a city…");
        cityField.setPrefWidth(200);
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

        Button saveFavoriteBtn = new Button("★ Favorite");
        saveFavoriteBtn.getStyleClass().add("button-secondary");
        saveFavoriteBtn.setOnAction(event -> saveCurrentAsFavorite());

        Button compareBtn = new Button("Compare");
        compareBtn.getStyleClass().add("button-secondary");
        compareBtn.setOnAction(event -> addCurrentToCompare());

        Button clearCompareBtn = new Button("Clear");
        clearCompareBtn.getStyleClass().add("button-danger");
        clearCompareBtn.setOnAction(event -> clearCompare());

        HBox searchRow = new HBox(10, cityField, cityQuickSelect, searchBtn, saveFavoriteBtn, compareBtn, clearCompareBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("search-bar");

        return new VBox(12, topRow, searchRow);
    }

    private HBox buildMainContent() {
        VBox hero = buildHeroCard();
        hero.setPrefWidth(300);
        hero.setMinWidth(260);

        ScrollPane hourlyScroll = forecastScroll(hourlyContainer, "Hourly forecast appears here.");
        VBox hourlyCard = card("Next 24 Hours", hourlyScroll);
        hourlyCard.setPrefWidth(280);
        HBox.setHgrow(hourlyCard, Priority.ALWAYS);

        ScrollPane dailyScroll = forecastScroll(dailyContainer, "7-day forecast appears here.");
        VBox dailyCard = card("7-Day Outlook", dailyScroll);
        dailyCard.setPrefWidth(280);
        HBox.setHgrow(dailyCard, Priority.ALWAYS);

        HBox main = new HBox(16, hero, hourlyCard, dailyCard);
        HBox.setHgrow(hero, Priority.NEVER);
        return main;
    }

    private ScrollPane forecastScroll(VBox container, String placeholder) {
        Label empty = new Label(placeholder);
        empty.getStyleClass().add("forecast-condition");
        container.getChildren().add(empty);

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefHeight(420);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private VBox buildHeroCard() {
        cityLabel.getStyleClass().add("city-name");
        regionLabel.getStyleClass().add("city-region");
        tempLabel.getStyleClass().add("temp-hero");
        conditionsLabel.getStyleClass().add("conditions-hero");
        clothingLabel.getStyleClass().add("clothing-badge");

        loadingIndicator.setMaxSize(28, 28);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);

        HBox titleRow = new HBox(12, loadingIndicator);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        GridPane stats = new GridPane();
        stats.setHgap(20);
        stats.setVgap(14);
        stats.add(statBlock("Humidity", humidityLabel), 0, 0);
        stats.add(statBlock("Wind", windLabel), 0, 1);
        stats.add(statBlock("UV Index", uvLabel), 0, 2);

        VBox hero = new VBox(16,
                titleRow,
                cityLabel,
                regionLabel,
                tempLabel,
                conditionsLabel,
                clothingLabel,
                stats
        );
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
        ListView<SavedLocation> favoritesList = locationList(favoritesItems, "No saved cities.");
        favoritesList.setOnMouseClicked(event -> {
            SavedLocation selected = favoritesList.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() >= 1) {
                loadLocation(selected);
            }
        });

        Button removeFavoriteBtn = new Button("Remove");
        removeFavoriteBtn.getStyleClass().add("button-danger");
        removeFavoriteBtn.setOnAction(event -> removeSelectedFavorite(favoritesList));

        VBox favoritesCard = card("Favorites", favoritesList, removeFavoriteBtn);
        VBox.setVgrow(favoritesList, Priority.ALWAYS);
        favoritesCard.setPrefWidth(250);
        favoritesCard.setMaxWidth(250);

        ListView<SavedLocation> historyList = locationList(historyItems, "No recent searches.");
        historyList.setOnMouseClicked(event -> {
            SavedLocation selected = historyList.getSelectionModel().getSelectedItem();
            if (selected != null && event.getClickCount() >= 1) {
                loadLocation(selected);
            }
        });

        VBox historyCard = card("Recent Searches", historyList);
        VBox.setVgrow(historyList, Priority.ALWAYS);
        historyCard.setPrefWidth(250);
        historyCard.setMaxWidth(250);

        VBox sidebar = new VBox(16, favoritesCard, historyCard);
        VBox.setVgrow(favoritesCard, Priority.ALWAYS);
        VBox.setVgrow(historyCard, Priority.ALWAYS);
        return sidebar;
    }

    private ListView<SavedLocation> locationList(ObservableList<SavedLocation> items, String placeholder) {
        ListView<SavedLocation> list = new ListView<>(items);
        list.setPlaceholder(new Label(placeholder));
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SavedLocation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        });
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
            if (node instanceof ScrollPane scrollPane) {
                VBox.setVgrow(scrollPane, Priority.ALWAYS);
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
        fetchAndRender(() -> weatherClient.fetchWeather(city), city);
    }

    private void loadLocation(SavedLocation location) {
        if (location.latitude() != 0 || location.longitude() != 0) {
            cityField.setText(location.city());
            fetchAndRender(() -> weatherClient.fetchWeather(
                    location.latitude(), location.longitude(),
                    location.city(), location.region(), location.country()
            ), location.displayName());
        } else {
            cityField.setText(location.city());
            searchWeather();
        }
    }

    private void fetchAndRender(WeatherFetcher fetcher, String label) {
        setLoading(true);
        setStatus("Loading " + label + "…");

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return fetcher.fetch();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(data -> Platform.runLater(() -> {
                    currentWeather = data;
                    currentLocation = SavedLocation.fromWeatherData(data);
                    try {
                        storage.saveSearchHistory(currentLocation);
                    } catch (IOException ignored) {
                    }
                    renderWeather(data);
                    refreshHistory();
                    setLoading(false);
                    setStatus("Updated · " + data.displayLocation());
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
        regionLabel.setText(buildRegionLine(data));
        tempLabel.setText(String.format("%.0f°", data.current().temperatureC()));
        conditionsLabel.setText(data.current().conditions());
        humidityLabel.setText(data.current().humidity() + "%");
        windLabel.setText(String.format("%.0f km/h", data.current().windSpeedKmh()));
        uvLabel.setText(String.format("%.1f", data.current().uvIndex()));
        clothingLabel.setText(clothingRecommendation(data.current().temperatureC(), data.current().conditions()));

        hourlyContainer.getChildren().clear();
        for (WeatherData.HourlyForecast hourly : data.hourlyForecast()) {
            hourlyContainer.getChildren().add(hourlyRow(
                    hourly.time().toLocalTime().toString().substring(0, 5),
                    String.format("%.0f°", hourly.temperatureC()),
                    hourly.conditions()
            ));
        }

        dailyContainer.getChildren().clear();
        for (WeatherData.DailyForecast daily : data.dailyForecast()) {
            dailyContainer.getChildren().add(dailyRow(
                    daily.date().toString(),
                    String.format("%.0f° / %.0f°", daily.maxTempC(), daily.minTempC()),
                    daily.conditions()
            ));
        }
    }

    private HBox hourlyRow(String time, String temp, String condition) {
        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("forecast-time");
        Label tempLabel = new Label(temp);
        tempLabel.getStyleClass().add("forecast-temp");
        Label condLabel = new Label(condition);
        condLabel.getStyleClass().add("forecast-condition");
        HBox row = new HBox(16, timeLabel, tempLabel, condLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("forecast-row");
        return row;
    }

    private HBox dailyRow(String date, String temps, String condition) {
        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("daily-date");
        Label tempLabel = new Label(temps);
        tempLabel.getStyleClass().add("daily-temps");
        Label condLabel = new Label(condition);
        condLabel.getStyleClass().add("forecast-condition");
        HBox row = new HBox(16, dateLabel, tempLabel, condLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("forecast-row");
        return row;
    }

    private String buildRegionLine(WeatherData data) {
        if (data.region() != null && !data.region().isBlank()) {
            return data.region() + ", " + data.country();
        }
        return data.country() != null ? data.country() : "";
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
    }

    private void saveCurrentAsFavorite() {
        if (currentLocation == null) {
            setStatus("Search for a city first.");
            return;
        }
        try {
            storage.addFavorite(currentLocation);
            refreshFavorites();
            setStatus("Saved " + currentLocation.displayName() + " to favorites.");
        } catch (IOException e) {
            setStatus("Couldn't save favorite: " + e.getMessage());
        }
    }

    private void removeSelectedFavorite(ListView<SavedLocation> favoritesList) {
        SavedLocation selected = favoritesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a favorite to remove.");
            return;
        }
        try {
            storage.removeFavorite(selected);
            refreshFavorites();
            setStatus("Removed " + selected.displayName() + ".");
        } catch (IOException e) {
            setStatus("Couldn't remove favorite: " + e.getMessage());
        }
    }

    private void addCurrentToCompare() {
        if (currentWeather == null) {
            setStatus("Search for a city to compare.");
            return;
        }
        boolean alreadyAdded = compareList.stream()
                .anyMatch(w -> w.latitude() == currentWeather.latitude()
                        && w.longitude() == currentWeather.longitude());
        if (alreadyAdded) {
            setStatus(currentWeather.displayLocation() + " is already in compare.");
            return;
        }
        if (compareList.size() >= MAX_COMPARE) {
            setStatus("Compare holds up to " + MAX_COMPARE + " cities. Clear to add more.");
            return;
        }
        compareList.add(currentWeather);
        renderCompare();
        setStatus("Added " + currentWeather.displayLocation() + " to compare.");
    }

    private void clearCompare() {
        compareList.clear();
        renderCompare();
        setStatus("Compare cleared.");
    }

    private void renderCompare() {
        compareRow.getChildren().clear();
        if (compareList.isEmpty()) {
            compareSection.setVisible(false);
            compareSection.setManaged(false);
            return;
        }

        compareSection.setVisible(true);
        compareSection.setManaged(true);

        Label compareTitle = new Label("COMPARE CITIES");
        compareTitle.getStyleClass().add("card-title");

        for (WeatherData data : compareList) {
            Label city = new Label(data.city());
            city.getStyleClass().add("compare-city");
            Label region = new Label(buildRegionLine(data));
            region.getStyleClass().add("compare-region");
            Label temp = new Label(String.format("%.0f°", data.current().temperatureC()));
            temp.getStyleClass().add("compare-temp");
            Label stats = new Label(String.format("%s · %d%% humidity · %.0f km/h wind",
                    data.current().conditions(), data.current().humidity(), data.current().windSpeedKmh()));
            stats.getStyleClass().add("compare-stat");
            stats.setWrapText(true);

            VBox card = new VBox(6, city, region, temp, stats);
            card.getStyleClass().add("compare-card");
            card.setOnMouseClicked(e -> {
                currentWeather = data;
                currentLocation = SavedLocation.fromWeatherData(data);
                renderWeather(data);
                setStatus("Showing " + data.displayLocation());
            });
            compareRow.getChildren().add(card);
        }

        compareSection.getChildren().setAll(compareTitle, compareRow);
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
            List<SavedLocation> history = storage.loadSearchHistory();
            List<SavedLocation> reversed = new ArrayList<>(history);
            java.util.Collections.reverse(reversed);
            historyItems.setAll(reversed);
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

    @FunctionalInterface
    private interface WeatherFetcher {
        WeatherData fetch() throws Exception;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
