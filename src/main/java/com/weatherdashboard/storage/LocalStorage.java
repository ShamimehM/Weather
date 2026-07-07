package com.weatherdashboard.storage;

import com.weatherdashboard.model.SavedLocation;

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

    public List<SavedLocation> loadFavorites() throws IOException {
        List<SavedLocation> favorites = new ArrayList<>();
        for (String line : readNonEmptyLines(favoritesFile)) {
            favorites.add(parseLine(line));
        }
        return favorites;
    }

    public void addFavorite(SavedLocation location) throws IOException {
        Set<String> lines = new LinkedHashSet<>();
        for (SavedLocation existing : loadFavorites()) {
            lines.add(existing.toStorageLine());
        }
        lines.add(location.toStorageLine());
        Files.write(favoritesFile, lines, StandardCharsets.UTF_8);
    }

    public void removeFavorite(SavedLocation location) throws IOException {
        Set<String> lines = new LinkedHashSet<>();
        String target = location.toStorageLine();
        for (SavedLocation existing : loadFavorites()) {
            if (!existing.toStorageLine().equals(target)) {
                lines.add(existing.toStorageLine());
            }
        }
        Files.write(favoritesFile, lines, StandardCharsets.UTF_8);
    }

    public void saveSearchHistory(SavedLocation location) throws IOException {
        String entry = LocalDateTime.now() + "|" + location.toStorageLine() + System.lineSeparator();
        Files.writeString(historyFile, entry, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
    }

    public List<SavedLocation> loadSearchHistory() throws IOException {
        List<SavedLocation> history = new ArrayList<>();
        for (String line : readNonEmptyLines(historyFile)) {
            int pipe = line.indexOf('|');
            if (pipe >= 0 && pipe < line.length() - 1) {
                history.add(parseLine(line.substring(pipe + 1)));
            }
        }
        return history;
    }

    private SavedLocation parseLine(String line) {
        if (line.contains("|") && line.matches("^-?\\d+\\.\\d+,-?\\d+\\.\\d+\\|.*")) {
            return SavedLocation.fromStorageLine(line);
        }
        return new SavedLocation(0, 0, line, "", "");
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
