# Weather App

Eine Spring Boot Anwendung zur Anzeige von Wetterdaten und Solarertragsprognosen.

## Setup

1. Kopiere `src/main/resources/application-example.properties` zu `src/main/resources/application.properties`
2. Trage deinen OpenWeatherMap API-Key in `application.properties` ein
3. Starte die Anwendung mit `./gradlew bootRun`

## Entwicklung

- Java 17 oder höher wird benötigt
- Gradle 8.5 oder höher (Wrapper ist im Projekt enthalten)
- IDE mit Spring Boot Support empfohlen (z.B. IntelliJ IDEA, Eclipse)

## Features

- Wetteranzeige für deutsche Städte
- Solarertragsprognose
- Dark Mode Support
- Interaktive Karte

## Build

```bash
./gradlew build
```

Das fertige JAR findet sich dann unter `build/libs/`. 