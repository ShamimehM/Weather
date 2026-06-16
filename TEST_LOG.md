# Weather Dashboard — Run & Test Log

**Date:** 2026-06-16  
**Project:** `weather dashbored`  
**Tester:** Cursor agent (automated + manual verification)

---

## 1. Environment setup

### Initial state
| Tool   | Status before |
|--------|---------------|
| Java   | Not on PATH   |
| Maven  | Not installed |

### Actions taken
1. Installed **Eclipse Temurin JDK 17** via winget:
   ```
   winget install EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements
   ```
   - Installed to: `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`

2. Downloaded **Apache Maven 3.9.6** locally (winget had no Maven package):
   - URL: `https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip`
   - Extracted to: `weather dashbored\.tools\apache-maven-3.9.6\`

3. Set session variables:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
   $env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
   $mvn = "d:\Codes\java\weather dashbored\.tools\apache-maven-3.9.6\bin\mvn.cmd"
   ```

---

## 2. Build

### Command
```powershell
cd "d:\Codes\java\weather dashbored"
& $mvn -q compile
```

### Result
**PASS** — exit code `0`  
Output saved to: `TEST_OUTPUT_COMPILE.txt`

---

## 3. Automated smoke tests

Added headless test class:  
`src/test/java/com/weatherdashboard/WeatherDashboardSmokeTest.java`

### Command
```powershell
& $mvn -q test-compile exec:java@smoke-test
```

### What was tested
| Area | Checks |
|------|--------|
| **Weather API** | Oslo fetch (temp, humidity, 24h hourly, 7-day daily) |
| | Tehran geocoding |
| | Invalid city throws `IllegalArgumentException` |
| **Local storage** | Add/remove favorites, dedupe, search history append |
| **Clothing logic** | Cold+rain, hot+clear, mild weather recommendations |

### Result
**PASS — 17/17 tests**

```
=== Weather Dashboard Smoke Test ===

[API] Fetching Oslo...
  Oslo: 22.3 C, Partly cloudy, humidity 47%
[API] Fetching Tehran...
  Tehran: 32.5 C, Clear sky
[API] Invalid city should fail... PASS

[Storage] Favorites and history... PASS
[Logic] Clothing recommendations... PASS

=== Results: 17 passed, 0 failed ===
```

Full output: `TEST_OUTPUT_SMOKE.txt`

---

## 4. Default city geocoding check

Verified all quick-select cities resolve via Open-Meteo geocoding API:

| City   | Result |
|--------|--------|
| Oslo   | OK → Oslo |
| Rasht  | OK → Rasht |
| Tehran | OK → Tehran |
| Shiraz | OK → Shiraz |
| Tabriz | OK → Tabriz |
| London | OK → London |
| Tokyo  | OK → Tokyo |
| Toronto| OK → Toronto |
| Sydney | OK → Sydney |

**PASS** — all 9 cities geocode successfully.

---

## 5. JavaFX GUI launch

### Command
```powershell
& $mvn -q javafx:run
```

### Result
**PASS** — application started successfully.

Evidence:
- Java processes appeared (`java.exe` PIDs observed after launch).
- Local data folder created on startup:
  ```
  .weather-dashboard/
    favorites.txt   (0 bytes — empty, expected before first save)
    history.txt     (0 bytes — empty, expected before first search)
  ```
- No crash or error output in the Maven/JavaFX process within ~2 minutes.

The GUI window should be visible on your desktop when running. Close it manually or stop the Maven process when done.

---

## 6. How to run yourself

### Prerequisites (one-time)
- JDK 17+ on PATH, **or** use the Temurin install above.
- Maven on PATH, **or** use the local `.tools\apache-maven-3.9.6\bin\mvn.cmd`.

### JavaFX dashboard (recommended)
```powershell
cd "d:\Codes\java\weather dashbored"
mvn javafx:run
```

### Console version
```powershell
mvn exec:java -Dexec.mainClass=com.weatherdashboard.WeatherDashboardApp
```

### Smoke tests only
```powershell
mvn test-compile exec:java@smoke-test
```

---

## 7. Manual UI checklist (for you)

After `mvn javafx:run`, verify in the window:

- [ ] Type **Oslo** → Search → current weather panel fills
- [ ] Hourly list shows 24 entries
- [ ] 7-day list shows 7 entries
- [ ] **Add Favorite** saves city; appears in Favorites panel
- [ ] Double-click favorite → loads weather again
- [ ] **Remove Favorite** removes selected city
- [ ] History panel shows timestamped searches
- [ ] Clothing recommendation updates (e.g. jacket/umbrella/shorts)
- [ ] Quick-select dropdown works for all 9 cities

---

## 8. Files produced by this test run

| File | Purpose |
|------|---------|
| `TEST_LOG.md` | This record |
| `TEST_OUTPUT_SMOKE.txt` | Smoke test console output |
| `TEST_OUTPUT_COMPILE.txt` | Compile output |
| `.tools/apache-maven-3.9.6/` | Local Maven install |
| `.weather-dashboard/` | App local storage (created on GUI start) |

---

## 9. Summary

| Step | Status |
|------|--------|
| JDK install | PASS |
| Maven setup | PASS |
| Compile | PASS |
| Smoke tests (API + storage + logic) | **17/17 PASS** |
| Default cities geocoding | **9/9 PASS** |
| JavaFX launch | PASS |

**Overall: project builds, tests pass, and the GUI starts correctly.**
