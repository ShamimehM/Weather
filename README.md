# Weather Dashboard

A Java weather dashboard that fetches live forecasts from the [Open-Meteo](https://open-meteo.com/) API. Available as a JavaFX desktop app or a console application.

## Features

- Search weather by city name
- Current conditions: temperature, humidity, wind, UV index
- 24-hour hourly forecast
- 7-day daily forecast
- Save favorite cities and view search history
- Clothing recommendations based on weather
- Quick-select for common cities

## Requirements

- JDK 17+
- Maven 3.9+

## Getting Started

Clone the repository and run:

```bash
mvn javafx:run
```

### Console mode

```bash
mvn exec:java -Dexec.mainClass=com.weatherdashboard.WeatherDashboardApp
```

### Smoke tests

```bash
mvn test-compile exec:java@smoke-test
```

## Project Structure

```
src/main/java/com/weatherdashboard/
├── WeatherDashboardFxApp.java   # JavaFX UI
├── WeatherDashboardApp.java     # Console UI
├── model/WeatherData.java       # Data models
├── service/OpenMeteoClient.java # API client
└── storage/LocalStorage.java    # Favorites & history
```

## License

This project is open source. Weather data provided by [Open-Meteo](https://open-meteo.com/).
