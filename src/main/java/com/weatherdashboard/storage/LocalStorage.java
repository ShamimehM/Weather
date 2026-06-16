package com.weatherdashboard.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LocalStorage {
    private final Path dataDir;
    private final Path favoritesFile;
    private final Path historyFile;

    public LocalStorage() throws IOException {
        this(Paths.get(".weather-dashboard"));
    }

    public LocalStorage(Path dataDir) throws IOException {
        this.dataDir = dataDir;
        this.favoritesFile = dataDir.resolve("favorites.txt");
        this.historyFile = dataDir.resolve("history.txt");
        if (!Files.exists(this.dataDir)) {
            Files.createDirectories(this.dataDir);
        }
        if (!Files.exists(favoritesFile)) {
            Files.writeString(favoritesFile, "", StandardCharsets.UTF_8);
        }
        if (!Files.exists(historyFile)) {
            Files.writeString(historyFile, "", StandardCharsets.UTF_8);
        }
    }

    public List<String> loadFavorites() throws IOException {
        return readNonEmptyLines(favoritesFile);
    }

    public void addFavorite(String city) throws IOException {
        Set<String> favorites = new LinkedHashSet<>(loadFavorites());
        favorites.add(city);
        Files.write(favoritesFile, favorites, StandardCharsets.UTF_8);
    }

    public void removeFavorite(String city) throws IOException {
        Set<String> favorites = new LinkedHashSet<>(loadFavorites());
        favorites.remove(city);
        Files.write(favoritesFile, favorites, StandardCharsets.UTF_8);
    }

    public void saveSearchHistory(String city) throws IOException {
        String entry = LocalDateTime.now() + " | " + city + System.lineSeparator();
        Files.writeString(historyFile, entry, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
    }

    public List<String> loadSearchHistory() throws IOException {
        return readNonEmptyLines(historyFile);
    }

    private List<String> readNonEmptyLines(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> filtered = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                filtered.add(line.trim());
            }
        }
        return filtered;
    }
}
