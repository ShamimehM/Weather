package com.weatherdashboard.model;

public record SavedLocation(
        double latitude,
        double longitude,
        String city,
        String region,
        String country
) {
    public String displayName() {
        StringBuilder sb = new StringBuilder(city);
        if (region != null && !region.isBlank()) {
            sb.append(", ").append(region);
        }
        if (country != null && !country.isBlank()) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }

    public String toStorageLine() {
        return latitude + "," + longitude + "|" + city + "|" + nullSafe(region) + "|" + nullSafe(country);
    }

    public static SavedLocation fromStorageLine(String line) {
        int pipe = line.indexOf('|');
        if (pipe < 0) {
            return new SavedLocation(0, 0, line.trim(), "", "");
        }
        String[] coords = line.substring(0, pipe).split(",");
        double lat = Double.parseDouble(coords[0]);
        double lon = Double.parseDouble(coords[1]);
        String[] parts = line.substring(pipe + 1).split("\\|", -1);
        String city = parts.length > 0 ? parts[0] : "";
        String region = parts.length > 1 ? parts[1] : "";
        String country = parts.length > 2 ? parts[2] : "";
        return new SavedLocation(lat, lon, city, region, country);
    }

    public static SavedLocation fromWeatherData(WeatherData data) {
        return new SavedLocation(
                data.latitude(),
                data.longitude(),
                data.city(),
                data.region(),
                data.country()
        );
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
