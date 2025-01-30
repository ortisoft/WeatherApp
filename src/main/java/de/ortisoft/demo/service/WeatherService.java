package de.ortisoft.demo.service;

import de.ortisoft.demo.config.WeatherConfig;
import de.ortisoft.demo.model.WeatherResponse;
import de.ortisoft.demo.model.ForecastResponse;
import de.ortisoft.demo.model.ForecastItem;
import de.ortisoft.demo.model.GeoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.DoubleSummaryStatistics;

@Service
public class WeatherService {
    private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_API_URL = "http://api.openweathermap.org/data/2.5/forecast";
    private static final String GEO_API_URL = "http://api.openweathermap.org/geo/1.0/reverse";
    
    private final WeatherConfig weatherConfig;
    private final RestTemplate restTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm", Locale.GERMAN);
    private final LocationService locationService;

    public WeatherService(WeatherConfig weatherConfig, LocationService locationService) {
        this.weatherConfig = weatherConfig;
        this.restTemplate = new RestTemplate();
        this.locationService = locationService;
    }

    public String getForecastByCoordinates(double lat, double lon) {
        try {
            String locationName = locationService.getLocationInfo(lat, lon);
            String url = UriComponentsBuilder.fromUriString(FORECAST_API_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", weatherConfig.getKey())
                .queryParam("units", "metric")
                .queryParam("lang", "de")
                .build()
                .toString();

            ForecastResponse response = restTemplate.getForObject(url, ForecastResponse.class);

            if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                StringBuilder forecast = new StringBuilder();
                forecast.append(String.format("""
                    <div class="forecast">
                        <h3>Wettervorhersage</h3>
                        <div class="forecast-days">
                    """));
                
                // Gruppiere Vorhersagen nach Tagen
                Map<LocalDate, List<ForecastItem>> dailyForecasts = response.getList().stream()
                    .collect(Collectors.groupingBy(item -> 
                        LocalDateTime.parse(item.getDt_txt(), 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .toLocalDate()
                    ));
                
                // Sortiere die Tage und zeige die ersten 5
                dailyForecasts.keySet().stream()
                    .sorted()
                    .limit(5)
                    .forEach(date -> {
                        List<ForecastItem> dayForecasts = dailyForecasts.get(date);
                        
                        // Berechne Tages-Zusammenfassung
                        DoubleSummaryStatistics tempStats = dayForecasts.stream()
                            .mapToDouble(item -> item.getMain().getTemp())
                            .summaryStatistics();
                        
                        // Durchschnittliche Bewölkung berechnen
                        double avgCloudCover = dayForecasts.stream()
                            .mapToInt(item -> item.getClouds().getAll())
                            .average()
                            .orElse(0);
                        
                        // Durchschnittliche Luftfeuchtigkeit berechnen
                        double avgHumidity = dayForecasts.stream()
                            .mapToInt(item -> item.getMain().getHumidity())
                            .average()
                            .orElse(0);
                        
                        // Häufigste Wetterbeschreibung finden
                        String commonDescription = dayForecasts.stream()
                            .map(item -> item.getWeather()[0].getDescription())
                            .collect(Collectors.groupingBy(desc -> desc, Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("");
                        
                        String dayName = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN));
                        String dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                        
                        // Haupteintrag für den Tag
                        forecast.append(String.format("""
                            <div class="forecast-day">
                                <div class="day-summary" onclick="toggleDetails('%s')">
                                    <div class="day-header">
                                        <span class="day-name">%s, %s</span>
                                        <span class="temp-range">🌡 %.1f°C bis %.1f°C</span>
                                        <span class="description">%s</span>
                                        <span class="cloud-cover">☁ %.0f%% Bewölkung</span>
                                        <span class="humidity">💧 %.0f%% Luftfeuchte</span>
                                    </div>
                                    <span class="toggle-icon">▼</span>
                                </div>
                                <div class="day-details" id="details-%s" style="display: none;">
                                    <table>
                                        <tr>
                                            <th>Uhrzeit</th>
                                            <th>Temperatur</th>
                                            <th>Beschreibung</th>
                                            <th>Bewölkung</th>
                                            <th>Luftfeuchtigkeit</th>
                                        </tr>
                        """, 
                        date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        dayName, dateStr,
                        tempStats.getMin(), tempStats.getMax(),
                        commonDescription,
                        avgCloudCover,
                        avgHumidity,
                        date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    ));
                    
                    // Details für jede Vorhersage des Tages
                    dayForecasts.forEach(item -> {
                        LocalDateTime dateTime = LocalDateTime.parse(item.getDt_txt(), 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        
                        forecast.append(String.format("""
                            <tr>
                                <td>%s</td>
                                <td>%.1f°C</td>
                                <td>%s</td>
                                <td>%d%%</td>
                                <td>%d%%</td>
                            </tr>
                            """,
                            dateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            item.getMain().getTemp(),
                            item.getWeather()[0].getDescription(),
                            item.getClouds().getAll(),
                            item.getMain().getHumidity()
                        ));
                    });
                    
                    forecast.append("""
                                </table>
                            </div>
                        </div>
                        """);
                    });
                
                forecast.append("</div>");
                
                forecast.append("""
        </div>
        <style>
            .forecast-days { margin-top: 20px; }
            .forecast-day { margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 5px; }
            .day-summary { 
                padding: 15px; 
                cursor: pointer; 
                background-color: var(--header-bg); 
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            .day-summary:hover { background-color: var(--hover-color); }
            .day-header { 
                display: grid;
                grid-template-columns: 150px 100px 150px repeat(3, 1fr);
                gap: 15px;
                align-items: start;
                width: 100%;
                line-height: 1.4;
            }
            .day-header > span {
                white-space: normal;
                min-height: 40px;
                display: flex;
                flex-direction: column;
                justify-content: center;
                color: var(--text-color);
            }
            .day-name { 
                font-weight: bold;
                white-space: nowrap !important;
                color: var(--text-color);
            }
            .radiation, .yield, .cloud-cover { 
                color: var(--subtitle-color);
            }
            .yield {
                font-size: 0.95em;
            }
            .toggle-icon { transition: transform 0.3s; }
            .day-details { 
                padding: 15px; 
                border-top: 1px solid var(--border-color); 
            }
            .day-details table { 
                width: 100%; 
                border-collapse: collapse; 
            }
            .day-details th, 
            .day-details td { 
                padding: 8px; 
                text-align: left; 
                border-bottom: 1px solid var(--border-color); 
            }
            .day-details th { 
                background-color: var(--header-bg);
                color: var(--text-color);
            }
            .settings {
                display: flex;
                flex-direction: column;
                gap: 15px;
                padding: 20px;
                background: #f8f8f8;
                border-radius: 8px;
                margin-bottom: 20px;
            }
            .settings h3 {
                margin: 0;
                color: #333;
            }
            .setting-group {
                display: flex;
                align-items: center;
                gap: 10px;
            }
            .total-yield {
                color: #666;
                font-weight: bold;
            }
            /* Grid Layout für die Anlageneinstellungen */
            .settings {
                padding: 20px;
                border-radius: 8px;
                margin-bottom: 20px;
                background-color: var(--header-bg);
            }
            .settings-grid {
                display: grid;
                grid-template-columns: 120px 1fr 1fr;
                gap: 15px;
                align-items: center;
                margin-bottom: 20px;
            }
            .setting-label {
                font-weight: bold;
                color: var(--text-color);
            }
            .setting-field {
                display: flex;
                align-items: center;
                gap: 5px;
            }
            .setting-field:first-of-type,
            .setting-field:nth-of-type(2) {
                font-weight: bold;
                font-size: 1.1em;
                padding-bottom: 10px;
                color: var(--subtitle-color);
            }
            .setting-field input,
            .setting-field select {
                padding: 8px;
                border-radius: 4px;
                width: 120px;
                background-color: var(--card-bg);
                color: var(--text-color);
                border: 1px solid var(--border-color);
            }
            /* Tab Styles */
            .tab-container {
                width: 100%;
                margin: 20px 0;
            }
            .tabs {
                display: flex;
                gap: 2px;
                background: var(--header-bg);
                padding: 2px;
                border-radius: 8px 8px 0 0;
            }
            .tab {
                padding: 12px 24px;
                cursor: pointer;
                border: none;
                border-radius: 8px 8px 0 0;
                font-size: 16px;
                background-color: var(--header-bg);
                color: var(--text-color);
            }
            .tab.active {
                background-color: var(--card-bg);
                font-weight: bold;
            }
            .tab-content {
                display: none;
                padding: 20px;
                background-color: var(--card-bg);
                border: 1px solid var(--border-color);
                border-radius: 0 0 8px 8px;
            }
            .tab-content.active {
                display: block;
            }
            /* Map Styles */
            .map-container {
                margin: 20px 0;
            }
            #map { 
                height: 400px; 
                width: 100%;
                border: 1px solid var(--border-color);
                border-radius: 8px;
            }
            
            /* Location Styles */
            .location-select {
                padding: 15px;
                border-radius: 8px;
                display: flex;
                gap: 10px;
                align-items: center;
                margin-bottom: 20px;
            }
            .location-select input {
                padding: 8px;
                border-radius: 4px;
                width: 200px;
            }
            .location-select button {
                padding: 8px 16px;
                border: none;
                border-radius: 4px;
                cursor: pointer;
                background-color: var(--button-bg);
                color: var(--button-text);
            }
            .location-select button:hover {
                background-color: var(--button-hover);
            }
            .or-divider {
                color: var(--text-color);
            }
            .current-weather table,
            .current-solar table,
            .forecast table {
                background-color: var(--card-bg);
                border: 1px solid var(--border-color);
                width: 100%;
                border-collapse: collapse;
                margin: 20px 0;
            }

            .current-weather th,
            .current-solar th,
            .forecast th {
                background-color: var(--header-bg);
                color: var(--text-color);
                border-bottom: 2px solid var(--border-color);
                padding: 12px 20px;  /* Erhöhtes Padding */
                text-align: left;
            }

            .current-weather td,
            .current-solar td,
            .forecast td {
                border: 1px solid var(--border-color);
                padding: 12px 20px;  /* Erhöhtes Padding */
                text-align: left;
            }

            .subtitle {
                color: var(--subtitle-color);
                font-size: 0.9em;
                font-style: italic;
                margin-top: 0;  /* Kein Abstand nach oben */
                margin-bottom: 25px;  /* Abstand zum nächsten Element */
            }
            
            .selected-location {
                color: var(--subtitle-color);
                margin-top: 20px;  /* Mehr Abstand nach oben */
                margin-bottom: 15px;  /* Konsistenter Abstand nach unten */
            }

            h1 {
                margin-bottom: 5px;  /* Reduzierter Abstand nach unten */
            }

            .chart-cell {
                width: 150px;
                padding: 5px !important;
            }
            .bar-container {
                width: 140px;
                height: 20px;
                background-color: #f0f0f0;  // Hellgrauer Hintergrund für STC-Maximum
                border-radius: 3px;
                position: relative;
                overflow: hidden;
            }
            .bar-max-day {
                position: absolute;
                left: 0;
                height: 100%;
                background-color: #e0e0e0;  // Helleres Grau für maximalen Tagesertrag
                border-radius: 3px;
            }
            .bar-max {
                position: absolute;
                left: 0;
                height: 100%;
                background-color: #ccc;  // Grau für sonnenhöhenbasiertes Maximum
                border-radius: 3px;
            }
            .bar-current {
                position: absolute;
                left: 0;
                height: 100%;
                background-color: #4CAF50;  // Grün für aktuellen Ertrag
                border-radius: 3px;
            }
            @media (prefers-color-scheme: dark) {
                .bar-container {
                    background-color: #2a2a2a;
                }
                .bar-max-day {
                    background-color: #3a3a3a;
                }
                .bar-max {
                    background-color: #444;
                }
                .bar-current {
                    background-color: #45a049;
                }
            }
            .max-yield-info {
                margin-top: 10px;
                padding: 10px;
                background-color: var(--card-bg);
                border: 1px solid var(--border-color);
                border-radius: 4px;
                color: var(--text-color);
            }
        </style>
        </div>
        """);

                return forecast.toString();
            }
            return "<p>Vorhersagedaten konnten nicht abgerufen werden.</p>";
        } catch (Exception e) {
            return String.format("<p>Fehler beim Abrufen der Vorhersage: %s</p>", e.getMessage());
        }
    }

    @Deprecated
    public String getSolarData(double lat, double lon, double kwp, int azimuth, int tilt) {
        return getSolarData(lat, lon, 
            kwp, azimuth, tilt, 20.0, 14.0,  // Standardwerte für efficiency und losses
            kwp, azimuth, tilt, 20.0, 14.0); // Gleiche Werte für zweite Anlage
    }

    @Deprecated
    public String getSolarData(double lat, double lon, 
                             double kwp1, int azimuth1, int tilt1,
                             double kwp2, int azimuth2, int tilt2) {
        return getSolarData(lat, lon,
            kwp1, azimuth1, tilt1, 20.0, 14.0,
            kwp2, azimuth2, tilt2, 20.0, 14.0);
    }

    public String getSolarData(double lat, double lon, 
                             double kwp1, int azimuth1, int tilt1, double efficiency1, double losses1,
                             double kwp2, int azimuth2, int tilt2, double efficiency2, double losses2) {
        try {
            StringBuilder solarInfo = new StringBuilder();
            
            // Hole die aktuelle Bewölkung
            String url = UriComponentsBuilder.fromUriString(WEATHER_API_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", weatherConfig.getKey())
                .queryParam("units", "metric")
                .queryParam("lang", "de")
                .build()
                .toString();

            WeatherResponse currentWeather = restTemplate.getForObject(url, WeatherResponse.class);
            double currentCloudCover = currentWeather != null && currentWeather.getClouds() != null ? 
                currentWeather.getClouds().getAll() : 0;

            // Berechnungsfaktoren anpassen
            double factor1 = calculateSolarFactor(azimuth1, tilt1) * (efficiency1/100.0) * (1.0 - losses1/100.0);
            double factor2 = calculateSolarFactor(azimuth2, tilt2) * (efficiency2/100.0) * (1.0 - losses2/100.0);

            solarInfo.append("""
                <div class="category">
                """);
            
            // Füge Einstellungen hinzu
            solarInfo.append(createSolarSettingsHtml());
            
            // Berechne beide maximalen Erträge
            double maxTheoretical = calculateMaxTheoretical(kwp1, efficiency1, losses1, 
                                                          kwp2, efficiency2, losses2);

            // Finde die maximale Sonnenhöhe des Tages
            final double maxSunHeight = calculateMaxSunHeight(lat, LocalDate.now());

            // Berechne die maximale theoretische Strahlung bei klarem Himmel
            double maxClearSkyRadiation = 1000.0 * Math.sin(Math.toRadians(maxSunHeight));

            // Berechne den maximalen Ertrag basierend auf der maximalen Sonnenhöhe
            double maxDayYield1 = calculateHourlyYield(maxClearSkyRadiation, efficiency1, losses1) * kwp1;
            double maxDayYield2 = calculateHourlyYield(maxClearSkyRadiation, efficiency2, losses2) * kwp2;
            double maxDayTotal = maxDayYield1 + maxDayYield2;

            solarInfo.append(String.format("""
                <div class="max-theoretical">
                    <h3>Maximaler Anlagenertrag</h3>
                    <div class="max-yield-info">
                        <div>Maximaler theoretischer Stundenertrag unter STC-Bedingungen (1000 W/m², 25°C): %.2f kWh</div>
                        <div>Maximaler Stundenertrag bei optimaler Ausrichtung (180° (Süd)) und Neigung (35°), klarem Himmel sowie maximaler Sonnenhöhe heute (%.1f°, %.0f W/m²): %.2f kWh</div>
                    </div>
                </div>
                """, maxTheoretical, maxSunHeight, maxClearSkyRadiation, maxDayTotal));


            // Füge Vorhersage hinzu
            solarInfo.append("""
                <div class="solar-forecast">
                    <h3>Prognose für die nächsten Tage*</h3>
                    <div class="forecast-days">
                """);

            // Vorhersagedaten abrufen
            String forecastUrl = UriComponentsBuilder.fromUriString(FORECAST_API_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", weatherConfig.getKey())
                .queryParam("units", "metric")
                .queryParam("lang", "de")
                .build()
                .toString();

            ForecastResponse forecastResponse = restTemplate.getForObject(forecastUrl, ForecastResponse.class);
            
            if (forecastResponse != null && forecastResponse.getList() != null) {
                // Gruppiere Vorhersagen nach Tagen
                Map<LocalDate, List<ForecastItem>> dailyForecasts = forecastResponse.getList().stream()
                    .collect(Collectors.groupingBy(item -> 
                        LocalDateTime.parse(item.getDt_txt(), 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .toLocalDate()
                    ));
                
                // Sortiere die Tage und zeige die ersten 5
                dailyForecasts.keySet().stream()
                    .sorted()
                    .limit(5)
                    .forEach(date -> {
                        List<ForecastItem> dayForecasts = dailyForecasts.get(date);
                        double avgCloudCover = dayForecasts.stream()
                            .mapToInt(item -> item.getClouds().getAll())
                            .average()
                            .orElse(0);
                        
                        double maxRadiation = calculateHourlyRadiation(lat, date, 12, avgCloudCover, azimuth1, tilt1);
                        double avgRadiation = calculateAverageRadiation(lat, date, avgCloudCover, azimuth1, tilt1);
                        double dailyYield1 = calculateDailyYield(lat, lon, avgCloudCover, date, azimuth1, tilt1, kwp1, efficiency1, losses1);
                        double dailyYield2 = calculateDailyYield(lat, lon, avgCloudCover, date, azimuth2, tilt2, kwp2, efficiency2, losses2);
                        
                        String dayName = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN));
                        String dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                        
                        solarInfo.append(String.format("""
                            <div class="forecast-day">
                                <div class="day-summary" onclick="toggleDetails('solar-%s')">
                                    <div class="day-header">
                                        <span class="day-name">%s, %s</span>
                                        <span class="cloud-cover">☁ %.0f%%</span>
                                        <span class="radiation">☀ %.0f - %.0f W/m² | Ø %.0f W/m²</span>
                                        <span class="yield">⚡ Anlage 1 (%.1f kWp): %.1f kWh</span>
                                        <span class="yield">⚡ Anlage 2 (%.1f kWp): %.1f kWh</span>
                                        <span class="total-yield">💡 Gesamt: %.1f kWh</span>
                                    </div>
                                    <span class="toggle-icon">▼</span>
                                </div>
                                <div class="day-details" id="details-solar-%s" style="display: none;">
                                    <table>
                                        <tr>
                                            <th>Uhrzeit</th>
                                            <th>Sonnenhöhe</th>
                                            <th>Bewölkung</th>
                                            <th>Strahlung (min-max | Ø)</th>
                                            <th>Anlage 1 (kWh/kWp)</th>
                                            <th>Anlage 1 (kWh)</th>
                                            <th>Anlage 2 (kWh/kWp)</th>
                                            <th>Anlage 2 (kWh)</th>
                                            <th>Gesamt (kWh)</th>
                                        </tr>
                                        %s
                                    </table>
                                </div>
                            </div>
                            """,
                            date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            dayName, dateStr,
                            avgCloudCover,
                            calculateMinRadiation(lat, date, avgCloudCover, azimuth1, tilt1), maxRadiation, avgRadiation,
                            kwp1, dailyYield1,
                            kwp2, dailyYield2,
                            dailyYield1 + dailyYield2,
                            date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            generateHourlyRows(lat, date, avgCloudCover, 
                                kwp1, azimuth1, tilt1, efficiency1, losses1,
                                kwp2, azimuth2, tilt2, efficiency2, losses2,
                                maxSunHeight)
                        ));
                    });
            }

            solarInfo.append("""
                    </div>
                </div>
                <div class="footnote" style="margin-top: 20px; font-size: 0.9em; border-top: 1px solid var(--border-color); padding-top: 10px;">
                    * Berechnungsgrundlagen der Solarprognose:
                    <ul>
                        <li>Strahlung (W/m²) = Grundstrahlung × cos(Sonnenwinkel) × (1 - Bewölkung/100 × 0.75)
                            <ul>
                                <li>Grundstrahlung: Theoretische maximale Strahlung bei klarem Himmel (ca. 1000 W/m²)</li>
                                <li>Bewölkung: Aus OpenWeatherMap-API (0-100%), reduziert die Strahlung um bis zu 75%</li>
                                <li>Ausrichtung (Azimut): 180° (Süd) ist optimal, Ost/West reduziert die effektive Strahlung um bis zu 30%</li>
                                <li>Neigung: 35° ist optimal, Abweichungen reduzieren die effektive Strahlung um bis zu 20%</li>
                            </ul>
                        </li>
                        <li>Anlagenertrag (kWh) = Strahlung × Anlagengröße × Wirkungsgrad × (1 - Verluste) × Zeit
                            <ul>
                                <li>Anlagengröße: kWp (Kilowatt Peak) bestimmt die maximale Leistung (1 kWp ≈ 5 m² Modulfläche bei 200 Wp/m²)</li>
                                <li>Wirkungsgrad: Moduleffizienz in % (typisch 15-22%)</li>
                                <li>Systemverluste: Kabel, Wechselrichter, Verschmutzung etc. (typisch 10-20%)</li>
                            </ul>
                        </li>
                    </ul>
                </div>
                </div>
                """);

            return solarInfo.toString();
        } catch (Exception e) {
            return String.format("<p>Fehler beim Abrufen der Solardaten: %s</p>", e.getMessage());
        }
    }

    private double calculateSunHeight(double lat, LocalDate date, int hour) {
        double dayOfYear = date.getDayOfYear();
        double declination = 23.45 * Math.sin(Math.toRadians((360.0/365.0) * (dayOfYear - 81)));
        double latRad = Math.toRadians(lat);
        double hourAngle = (hour - 12) * 15; // 15° pro Stunde
        double hourAngleRad = Math.toRadians(hourAngle);
        double declinationRad = Math.toRadians(declination);
        
        double sinHeight = Math.sin(latRad) * Math.sin(declinationRad) +
                          Math.cos(latRad) * Math.cos(declinationRad) * Math.cos(hourAngleRad);
        
        return Math.toDegrees(Math.asin(sinHeight));
    }

    private double calculateSunAzimuth(double lat, LocalDate date, int hour) {
        double dayOfYear = date.getDayOfYear();
        double declination = 23.45 * Math.sin(Math.toRadians((360.0/365.0) * (dayOfYear - 81)));
        double latRad = Math.toRadians(lat);
        double decRad = Math.toRadians(declination);
        double hourAngle = (hour - 12) * 15;
        double hourAngleRad = Math.toRadians(hourAngle);
        
        // Berechne Sonnenhöhe
        double sinHeight = Math.sin(latRad) * Math.sin(decRad) +
                          Math.cos(latRad) * Math.cos(decRad) * Math.cos(hourAngleRad);
        double sunHeight = Math.asin(sinHeight);
        
        // Wenn die Sonne unter dem Horizont ist, gib 0 zurück
        if (Math.toDegrees(sunHeight) <= 0) {
            return 0;
        }
        
        // Berechne Azimut
        double cosAzimuth = (Math.sin(decRad) - Math.sin(latRad) * sinHeight) /
                           (Math.cos(latRad) * Math.cos(sunHeight));
        
        // Begrenze cosAzimuth auf [-1, 1] um NaN zu vermeiden
        cosAzimuth = Math.max(-1, Math.min(1, cosAzimuth));
        
        double azimuth = Math.toDegrees(Math.acos(cosAzimuth));
        if (hour > 12) {
            azimuth = 360 - azimuth;
        }
        
        return azimuth;
    }

    private double calculateOrientationFactor(double sunHeight, double sunAzimuth, 
                                            double panelAzimuth, double panelTilt) {
        // Wenn die Sonne unter dem Horizont ist, kein Ertrag
        if (sunHeight <= 0) return 0;
        
        // Umrechnung in Radiant
        double sunHeightRad = Math.toRadians(sunHeight);
        double sunAzimuthRad = Math.toRadians(sunAzimuth);
        double panelAzimuthRad = Math.toRadians(panelAzimuth);
        double panelTiltRad = Math.toRadians(panelTilt);
        
        // Berechnung des Einfallswinkels
        double cosIncidence = Math.sin(sunHeightRad) * Math.cos(panelTiltRad) +
                            Math.cos(sunHeightRad) * Math.sin(panelTiltRad) * 
                            Math.cos(sunAzimuthRad - panelAzimuthRad);
        
        // Korrektur für negative Werte (Rückseite des Panels)
        return Math.max(0, cosIncidence);
    }

    public double calculateHourlyYield(double radiation, double efficiency, double losses) {
        // 1. Umrechnung von W/m² in kWh/m²
        double kwhPerM2 = radiation / 1000.0;  // Eine Stunde = 1/1000 kWh/W
        
        // 2. Umrechnung von kWh/m² in kWh/kWp (1 kWp ≈ 5 m²)
        double kwhPerKwp = kwhPerM2 * 5.0;
        
        // 3. Anwendung von Wirkungsgrad und Verlusten
        return kwhPerKwp * (efficiency/100.0) * (1.0 - losses/100.0);
    }

    private double calculateHourlyRadiation(double lat, LocalDate date, int hour, double cloudCover, int azimuth, int tilt) {
        double sunHeight = calculateSunHeight(lat, date, hour);
        if (sunHeight <= 0) return 0;
        
        double sunAzimuth = calculateSunAzimuth(lat, date, hour);
        
        // "Eine Sonneneinstrahlung von 1.000 Watt pro m²" (STC)
        double maxRadiation = 1000.0 * Math.sin(Math.toRadians(sunHeight));
        
        // Bewölkungs-Faktor
        // "Grundsätzlich funktioniert Photovoltaik auch im Schatten"
        double cloudFactor = 1.0 - (cloudCover / 100.0) * 0.70; // 30% Minimum bei voller Bewölkung
        
        // Ausrichtungs- und Neigungsfaktor
        // "In Deutschland ist bei einem Dach mit südlicher Ausrichtung ein 
        // Neigungswinkel zwischen 30 und 45 Grad ideal"
        double orientationFactor = calculateOrientationFactor(sunHeight, sunAzimuth, azimuth, tilt);
        
        // Temperatureinfluss
        // "Sobald die Solarzellen eine höhere Temperatur als die 25 °C erreichen, 
        // sinkt die Leistung um 0,3 - 0,4% pro Grad"
        double tempFactor = 1.0;  // Standardtemperatur
        if (hour >= 10 && hour <= 16) {  // Während der heißen Tageszeit
            tempFactor = 0.90;    // Annahme: ca. 10% Verlust durch Temperatur
        }
        
        return maxRadiation * cloudFactor * orientationFactor * tempFactor;
    }

    private double calculateDailyYield(double lat, double lon, double avgCloudCover, LocalDate date, 
                                     int azimuth, int tilt, double kwp, double efficiency, double losses) {
        double dailyYield = 0;
        for (int hour = 0; hour < 24; hour++) {
            double hourlyRadiation = calculateHourlyRadiation(lat, date, hour, avgCloudCover, azimuth, tilt);
            dailyYield += calculateHourlyYield(hourlyRadiation, efficiency, losses) * kwp;
        }
        return dailyYield;
    }

    private double calculateAverageRadiation(double lat, LocalDate date, double avgCloudCover, int azimuth, int tilt) {
        double totalRadiation = 0;
        int dayHours = 0;
        for (int hour = 0; hour < 24; hour++) {
            double sunHeight = calculateSunHeight(lat, date, hour);
            if (sunHeight > 0) {
                totalRadiation += calculateHourlyRadiation(lat, date, hour, avgCloudCover, azimuth, tilt);
                dayHours++;
            }
        }
        return dayHours > 0 ? totalRadiation / dayHours : 0;
    }

    public String getWeatherAndForecast() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Wetterbericht</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <style>
                    :root {
                        --bg-color: #ffffff;
                        --text-color: #333333;
                        --border-color: #dddddd;
                        --hover-color: #f5f5f5;
                        --header-bg: #f8f8f8;
                        --card-bg: #ffffff;
                        --button-bg: #4CAF50;
                        --button-hover: #45a049;
                        --button-text: #ffffff;
                        --subtitle-color: #666666;
                    }

                    @media (prefers-color-scheme: dark) {
                        :root {
                            --bg-color: #1a1a1a;
                            --text-color: #e0e0e0;
                            --border-color: #404040;
                            --hover-color: #2d2d2d;
                            --header-bg: #2d2d2d;
                            --card-bg: #262626;
                            --button-bg: #45a049;
                            --button-hover: #4CAF50;
                            --button-text: #ffffff;
                            --subtitle-color: #b0b0b0;
                        }
                    }

                    body {
                        font-family: Arial, sans-serif;
                        margin: 20px;
                        background-color: var(--bg-color);
                        color: var(--text-color);
                    }

                    .current-weather table,
                    .current-solar table,
                    .forecast table {
                        background-color: var(--card-bg);
                        border: 1px solid var(--border-color);
                        width: 100%;
                        border-collapse: collapse;
                        margin: 20px 0;
                    }

                    .current-weather th,
                    .current-solar th,
                    .forecast th {
                        background-color: var(--header-bg);
                        color: var(--text-color);
                        border-bottom: 2px solid var(--border-color);
                        padding: 12px 20px;  /* Erhöhtes Padding */
                        text-align: left;
                    }

                    .current-weather td,
                    .current-solar td,
                    .forecast td {
                        border: 1px solid var(--border-color);
                        padding: 12px 20px;  /* Erhöhtes Padding */
                        text-align: left;
                    }

                    .current-weather tr:hover,
                    .current-solar tr:hover:not(.total-row),
                    .forecast tr:hover {
                        background-color: var(--hover-color);
                    }

                    .settings {
                        background-color: var(--header-bg);
                    }

                    .settings button {
                        background-color: var(--button-bg);
                        color: var(--button-text);
                    }

                    .settings button:hover {
                        background-color: var(--button-hover);
                    }

                    .location-select {
                        background-color: var(--header-bg);
                    }

                    .location-select input {
                        background-color: var(--card-bg);
                        color: var(--text-color);
                        border: 1px solid var(--border-color);
                    }

                    .tab {
                        background-color: var(--header-bg);
                        color: var(--text-color);
                    }

                    .tab.active {
                        background-color: var(--card-bg);
                    }

                    .tab-content {
                        background-color: var(--card-bg);
                        border: 1px solid var(--border-color);
                    }

                    .subtitle {
                        color: var(--subtitle-color);
                        font-size: 0.9em;
                        font-style: italic;
                        margin-top: 0;  /* Kein Abstand nach oben */
                        margin-bottom: 25px;  /* Abstand zum nächsten Element */
                    }

                    .selected-location {
                        color: var(--subtitle-color);
                        margin-top: 20px;  /* Mehr Abstand nach oben */
                        margin-bottom: 15px;  /* Konsistenter Abstand nach unten */
                    }

                    .forecast-day {
                        margin-bottom: 10px; 
                        border: 1px solid var(--border-color); 
                        border-radius: 5px; 
                    }
                    .day-summary { 
                        padding: 15px; 
                        cursor: pointer; 
                        background-color: var(--header-bg); 
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .day-summary:hover { 
                        background-color: var(--hover-color); 
                    }
                    .day-header { 
                        display: grid;
                        grid-template-columns: 150px 100px 150px repeat(3, 1fr);
                        gap: 15px;
                        align-items: start;
                        width: 100%;
                        line-height: 1.4;
                    }
                    .day-header > span {
                        white-space: normal;
                        min-height: 40px;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        color: var(--text-color);
                    }
                    .day-name { 
                        font-weight: bold;
                        white-space: nowrap !important;
                        color: var(--text-color);
                    }
                    .radiation, .yield, .cloud-cover { 
                        color: var(--subtitle-color);
                    }
                    /* Tab Styles */
                    .tab-container {
                        width: 100%;
                        margin: 20px 0;
                    }
                    .tabs {
                        display: flex;
                        gap: 2px;
                        background: var(--header-bg);
                        padding: 2px;
                        border-radius: 8px 8px 0 0;
                    }
                    .tab {
                        padding: 12px 24px;
                        cursor: pointer;
                        border: none;
                        border-radius: 8px 8px 0 0;
                        font-size: 16px;
                        background-color: var(--header-bg);
                        color: var(--text-color);
                    }
                    .tab.active {
                        background-color: var(--card-bg);
                        font-weight: bold;
                    }
                    .tab-content {
                        display: none;
                        padding: 20px;
                        background-color: var(--card-bg);
                        border: 1px solid var(--border-color);
                        border-radius: 0 0 8px 8px;
                    }
                    .tab-content.active {
                        display: block;
                    }
                    /* Map Styles */
                    .map-container {
                        margin: 20px 0;
                    }
                    #map { 
                        height: 400px; 
                        width: 100%;
                        border: 1px solid var(--border-color);
                        border-radius: 8px;
                    }
                    
                    /* Location Styles */
                    .location-select {
                        padding: 15px;
                        border-radius: 8px;
                        display: flex;
                        gap: 10px;
                        align-items: center;
                        margin-bottom: 20px;
                    }
                    .location-select input {
                        padding: 8px;
                        border-radius: 4px;
                        width: 200px;
                    }
                    .location-select button {
                        padding: 8px 16px;
                        border: none;
                        border-radius: 4px;
                        cursor: pointer;
                        background-color: var(--button-bg);
                        color: var(--button-text);
                    }
                    .location-select button:hover {
                        background-color: var(--button-hover);
                    }
                    .or-divider {
                        color: var(--text-color);
                    }

                    .chart-cell {
                        width: 150px;
                        padding: 5px !important;
                    }
                    .bar-container {
                        width: 140px;
                        height: 20px;
                        background-color: #f0f0f0;  // Hellgrauer Hintergrund für STC-Maximum
                        border-radius: 3px;
                        position: relative;
                        overflow: hidden;
                    }
                    .bar-max-day {
                        position: absolute;
                        left: 0;
                        height: 100%;
                        background-color: #e0e0e0;  // Helleres Grau für maximalen Tagesertrag
                        border-radius: 3px;
                    }
                    .bar-max {
                        position: absolute;
                        left: 0;
                        height: 100%;
                        background-color: #ccc;  // Grau für sonnenhöhenbasiertes Maximum
                        border-radius: 3px;
                    }
                    .bar-current {
                        position: absolute;
                        left: 0;
                        height: 100%;
                        background-color: #4CAF50;  // Grün für aktuellen Ertrag
                        border-radius: 3px;
                    }
                    @media (prefers-color-scheme: dark) {
                        .bar-container {
                            background-color: #2a2a2a;
                        }
                        .bar-max-day {
                            background-color: #3a3a3a;
                        }
                        .bar-max {
                            background-color: #444;
                        }
                        .bar-current {
                            background-color: #45a049;
                        }
                    }
                    .max-yield-info {
                        margin-top: 10px;
                        padding: 10px;
                        background-color: var(--card-bg);
                        border: 1px solid var(--border-color);
                        border-radius: 4px;
                        color: var(--text-color);
                    }
                </style>
            </head>
            <body>
                <h1>Wetter- und Solarertragsprognose</h1>
                <div class="subtitle">Coded completely by AI with human guidance (with Cursor AI)</div>
                <div id="selected-location" class="selected-location">Kein Ort ausgewählt</div>
                
                <div class="tab-container">
                    <div class="tabs">
                        <button class="tab active" onclick="openTab('location')">📍 Standort</button>
                        <button class="tab" onclick="openTab('weather')">🌤 Wetter</button>
                        <button class="tab" onclick="openTab('solar')">☀️ Solarertrag</button>
                    </div>
                    
                    <div id="location-tab" class="tab-content active">
                        <div class="location-options">
                            <div class="location-select">
                                <input type="text" id="city" placeholder="Stadt eingeben (z.B. Berlin)" onkeypress="handleKeyPress(event)">
                                <button onclick="getWeatherByCity()">Stadt suchen</button>
                                <span class="or-divider">oder</span>
                                <button onclick="updateWeather()">Aktuellen Standort verwenden</button>
                            </div>
                        </div>
                        
                        <div class="map-container">
                            <h3>Oder wählen Sie einen Standort auf der Karte:</h3>
                            <div id="map"></div>
                        </div>
                    </div>
                    
                    <div id="weather-tab" class="tab-content">
                        <div id="weather-data"></div>
                    </div>
                    
                    <div id="solar-tab" class="tab-content">
                        <div id="solar-data"></div>
                    </div>
                </div>
                
                <div id="loading" style="display: none;">Standort wird ermittelt...</div>
                <div id="error"></div>
                
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script>
                    // Am Anfang des JavaScript-Teils:
                    let lastLat, lastLon;
                    let currentKwp1 = 4.8, currentKwp2 = 4.8;
                    let currentEfficiency1 = 20.0, currentEfficiency2 = 20.0;
                    let currentLosses1 = 14.0, currentLosses2 = 14.0;
                    let currentTab = 'location';  // Initialisiere mit dem Standard-Tab

                    function openTab(tabName) {
                        // Alle Tab-Inhalte ausblenden
                        const tabContents = document.getElementsByClassName('tab-content');
                        for (let content of tabContents) {
                            content.classList.remove('active');
                        }
                        
                        // Alle Tab-Buttons deaktivieren
                        const tabs = document.getElementsByClassName('tab');
                        for (let tab of tabs) {
                            tab.classList.remove('active');
                        }
                        
                        // Gewählten Tab und Content aktivieren
                        document.getElementById(tabName + '-tab').classList.add('active');
                        const clickedTab = document.querySelector(`button[onclick="openTab('${tabName}')"]`);
                        if (clickedTab) {
                            clickedTab.classList.add('active');
                        }
                    }

                    // Initial den Location-Tab öffnen
                    document.addEventListener('DOMContentLoaded', function() {
                        openTab('location');
                    });

                    function loadWeatherData(lat, lon, kwp1 = 4.8, kwp2 = 4.8) {
                        // Speichere die aktuellen Werte
                        window.lastLat = lat;
                        window.lastLon = lon;
                        
                        // Hole die aktuellen Anlagenparameter mit Standardwerten
                        const azimuth1 = document.getElementById('azimuth1')?.value || 90;
                        const tilt1 = document.getElementById('tilt1')?.value || 18;
                        const azimuth2 = document.getElementById('azimuth2')?.value || 270;
                        const tilt2 = document.getElementById('tilt2')?.value || 18;
                        const efficiency1 = document.getElementById('efficiency1')?.value || window.currentEfficiency1 || 20.0;
                        const efficiency2 = document.getElementById('efficiency2')?.value || window.currentEfficiency2 || 20.0;
                        const losses1 = document.getElementById('losses1')?.value || window.currentLosses1 || 14.0;
                        const losses2 = document.getElementById('losses2')?.value || window.currentLosses2 || 14.0;

                        // Aktualisiere die globalen Werte
                        window.currentKwp1 = kwp1;
                        window.currentKwp2 = kwp2;
                        window.currentEfficiency1 = efficiency1;
                        window.currentEfficiency2 = efficiency2;
                        window.currentLosses1 = losses1;
                        window.currentLosses2 = losses2;

                        const clientTime = new Date();
                        const clientOffset = -clientTime.getTimezoneOffset();
                        const clientTimeISO = clientTime.toISOString();

                        fetch(`/weather?lat=${lat}&lon=${lon}&clientTime=${clientTimeISO}&clientOffset=${clientOffset}&` +
                              `kwp1=${kwp1}&azimuth1=${azimuth1}&tilt1=${tilt1}&efficiency1=${efficiency1}&losses1=${losses1}&` +
                              `kwp2=${kwp2}&azimuth2=${azimuth2}&tilt2=${tilt2}&efficiency2=${efficiency2}&losses2=${losses2}`)
                            .then(response => {
                                console.log('Response Status:', response.status);
                                return response.text();
                            })
                            .then(html => {
                                console.log('Rohe Server-Antwort:', html);
                                
                                const weatherData = document.getElementById('weather-data');
                                const solarData = document.getElementById('solar-data');
                                
                                // Temporärer Container für das Parsen
                                const tempDiv = document.createElement('div');
                                tempDiv.innerHTML = html;
                                
                                // Alle Kategorien ausgeben
                                const allCategories = tempDiv.querySelectorAll('.category');
                                console.log('Anzahl gefundener Kategorien:', allCategories.length);
                                allCategories.forEach((cat, index) => {
                                    console.log(`Kategorie ${index + 1}:`, cat.outerHTML);
                                });
                                
                                // Direkte Suche nach dem Wetter- und Solar-Content
                                const weatherContent = tempDiv.querySelector('.category');
                                const solarContent = tempDiv.querySelector('.category + .category');  // Wählt die zweite .category
                                
                                console.log('Weather-Content gefunden:', !!weatherContent);
                                console.log('Solar-Content gefunden:', !!solarContent);
                                
                                // Verteile den Inhalt auf die richtigen Tabs
                                if (weatherContent && weatherData) {
                                    console.log('Füge Weather-Content ein');
                                    weatherData.innerHTML = weatherContent.outerHTML;
                                    
                                    // Extrahiere den Ortsnamen aus dem location-info Element
                                    const locationInfo = weatherContent.querySelector('.location-info');
                                    if (locationInfo) {
                                        const locationName = locationInfo.dataset.location;
                                        const now = new Date();
                                        const dateStr = now.toLocaleDateString('de-DE');
                                        const timeStr = now.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
                                        
                                        // Aktualisiere den Titel mit Koordinaten
                                        document.getElementById('selected-location').textContent = 
                                            `Ausgewählter Standort: ${locationName} (${lat.toFixed(6)}°, ${lon.toFixed(6)}°) - ${dateStr}, ${timeStr} Uhr`;
                                    }
                                } else {
                                    console.error('Weather-Content nicht gefunden');
                                }
                                
                                if (solarContent && solarData) {
                                    console.log('Füge Solar-Content ein');
                                    solarData.innerHTML = solarContent.outerHTML;
                                } else {
                                    console.error('Solar-Content nicht gefunden');
                                    console.log('Solar-Container vorhanden:', !!solarData);
                                    console.log('Solar-Content vorhanden:', !!solarContent);
                                }
                                
                                document.getElementById('loading').style.display = 'none';
                                document.getElementById('error').style.display = 'none';
                                
                                // Bleibe im aktuellen Tab
                                const currentTabButton = document.querySelector('.tab.active');
                                if (currentTabButton) {
                                    const tabName = currentTabButton.getAttribute('onclick').match(/'([^']+)'/)[1];
                                    openTab(tabName);
                                }
                                
                                // Aktualisiere die Eingabefelder
                                const kwp1Input = document.getElementById('kwp1');
                                const kwp2Input = document.getElementById('kwp2');
                                if (kwp1Input) {
                                    kwp1Input.value = window.currentKwp1;
                                    window.currentKwp1 = kwp1;
                                }
                                if (kwp2Input) {
                                    kwp2Input.value = window.currentKwp2;
                                    window.currentKwp2 = kwp2;
                                }
                                const azimuth1Select = document.getElementById('azimuth1');
                                const tilt1Input = document.getElementById('tilt1');
                                const azimuth2Select = document.getElementById('azimuth2');
                                const tilt2Input = document.getElementById('tilt2');
                                
                                if (azimuth1Select) azimuth1Select.value = azimuth1;
                                if (tilt1Input) tilt1Input.value = tilt1;
                                if (azimuth2Select) azimuth2Select.value = azimuth2;
                                if (tilt2Input) tilt2Input.value = tilt2;

                                // Nach den bestehenden Eingabefeld-Updates:
                                const efficiency1Input = document.getElementById('efficiency1');
                                const efficiency2Input = document.getElementById('efficiency2');
                                const losses1Input = document.getElementById('losses1');
                                const losses2Input = document.getElementById('losses2');

                                if (efficiency1Input) {
                                    efficiency1Input.value = window.currentEfficiency1;
                                    window.currentEfficiency1 = efficiency1;
                                }
                                if (efficiency2Input) {
                                    efficiency2Input.value = window.currentEfficiency2;
                                    window.currentEfficiency2 = efficiency2;
                                }
                                if (losses1Input) {
                                    losses1Input.value = window.currentLosses1;
                                    window.currentLosses1 = losses1;
                                }
                                if (losses2Input) {
                                    losses2Input.value = window.currentLosses2;
                                    window.currentLosses2 = losses2;
                                }
                            })
                            .catch(error => {
                                console.error('Fehler beim Laden der Daten:', error);
                                showError('Fehler beim Laden der Wetterdaten: ' + error);
                            });
                    }
                    
                    // Definiere die Koordinaten der verfügbaren Städte
                    const coordinates = {
                        'berlin': { lat: 52.520008, lon: 13.404954 },
                        'hamburg': { lat: 53.551086, lon: 9.993682 },
                        'münchen': { lat: 48.137154, lon: 11.576124 },
                        'köln': { lat: 50.937531, lon: 6.960279 },
                        'frankfurt': { lat: 50.110924, lon: 8.682127 },
                        'stuttgart': { lat: 48.775846, lon: 9.182932 },
                        'düsseldorf': { lat: 51.227741, lon: 6.773456 },
                        'leipzig': { lat: 51.339695, lon: 12.373075 },
                        'dortmund': { lat: 51.513587, lon: 7.465298 },
                        'essen': { lat: 51.455643, lon: 7.011555 },
                        'bremen': { lat: 53.079296, lon: 8.801694 },
                        'dresden': { lat: 51.050409, lon: 13.737262 },
                        'hannover': { lat: 52.375892, lon: 9.732010 },
                        'nürnberg': { lat: 49.452102, lon: 11.076665 },
                        'duisburg': { lat: 51.434408, lon: 6.762329 },
                        'bochum': { lat: 51.481845, lon: 7.216236 },
                        'wuppertal': { lat: 51.256213, lon: 7.150764 },
                        'bielefeld': { lat: 52.030228, lon: 8.532471 },
                        'bonn': { lat: 50.737430, lon: 7.098207 },
                        'münster': { lat: 51.960665, lon: 7.626135 },
                        'karlsruhe': { lat: 49.006890, lon: 8.403653 },
                        'mannheim': { lat: 49.487459, lon: 8.466039 },
                        'augsburg': { lat: 48.366512, lon: 10.894446 },
                        'wiesbaden': { lat: 50.082085, lon: 8.239761 },
                        'gelsenkirchen': { lat: 51.517744, lon: 7.085717 },
                        'mönchengladbach': { lat: 51.196768, lon: 6.442761 },
                        'braunschweig': { lat: 52.269053, lon: 10.520940 },
                        'chemnitz': { lat: 50.827845, lon: 12.921369 },
                        'kiel': { lat: 54.322719, lon: 10.135412 },
                        'aachen': { lat: 50.776351, lon: 6.083862 },
                        'halle': { lat: 51.482778, lon: 11.969444 },
                        'magdeburg': { lat: 52.130870, lon: 11.627624 },
                        'freiburg': { lat: 47.997791, lon: 7.842609 },
                        'krefeld': { lat: 51.334419, lon: 6.564279 },
                        'mainz': { lat: 50.016667, lon: 8.266667 },
                        'lübeck': { lat: 53.866667, lon: 10.683333 },
                        'erfurt': { lat: 50.978056, lon: 11.029167 },
                        'rostock': { lat: 54.083333, lon: 12.133333 },
                        'kassel': { lat: 51.316667, lon: 9.500000 },
                        'potsdam': { lat: 52.400833, lon: 13.066667 },
                        'schwerin': { lat: 53.633333, lon: 11.416667 },
                        'boizenburg': { lat: 53.3667, lon: 10.7167 },  // Boizenburg/Elbe
                        'wiesbaden': { lat: 50.082085, lon: 8.239761 }
                    };

                    function getWeatherData(position) {
                        const lat = position.coords.latitude;
                        const lon = position.coords.longitude;
                        window.currentKwp1 = document.getElementById('kwp1')?.value || window.currentKwp1 || 9.6;
                        window.currentKwp2 = document.getElementById('kwp2')?.value || window.currentKwp2 || 9.6;
                        
                        document.getElementById('loading').textContent = 'Wetterdaten werden geladen...';
                        loadWeatherData(lat, lon, window.currentKwp1, window.currentKwp2);
                    }

                    function getWeatherByCity() {
                        const city = document.getElementById('city').value.trim().toLowerCase();
                        if (!city) {
                            alert('Bitte geben Sie eine Stadt ein.');
                            return;
                        }
                        
                        if (coordinates[city]) {
                            loadWeatherData(coordinates[city].lat, coordinates[city].lon, window.currentKwp1 || 9.6, window.currentKwp2 || 9.6);
                        } else {
                            showError('Stadt nicht gefunden. Bitte geben Sie eine deutsche Großstadt ein.');
                        }
                    }

                    function updateWeather() {
                        if (!marker) {
                            console.error('Kein Standort ausgewählt');
                            return;
                        }
                        
                        const lat = marker.getLatLng().lat;
                        const lng = marker.getLatLng().lng;
                        const kwp1 = document.getElementById('kwp1').value;
                        const kwp2 = document.getElementById('kwp2').value;
                        const azimuth1 = document.getElementById('azimuth1').value;
                        const azimuth2 = document.getElementById('azimuth2').value;
                        const tilt1 = document.getElementById('tilt1').value;
                        const tilt2 = document.getElementById('tilt2').value;
                        const efficiency1 = document.getElementById('efficiency1').value;
                        const efficiency2 = document.getElementById('efficiency2').value;
                        const losses1 = document.getElementById('losses1').value;
                        const losses2 = document.getElementById('losses2').value;

                        // Lade die kompletten Wetterdaten neu
                        loadWeatherData(lat, lng, kwp1, kwp2, azimuth1, azimuth2, tilt1, tilt2, efficiency1, efficiency2, losses1, losses2);
                    }

                    // Karte initialisieren
                    const map = L.map('map').setView([51.165691, 10.451526], 6);
                    
                    // Nur ein Layer für beide Modi
                    const tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);
                    
                    // Marker initialisieren, aber noch nicht zur Karte hinzufügen
                    let marker = null;
                    
                    // Klick-Handler für die Karte
                    map.on('click', function(e) {
                        const lat = e.latlng.lat;
                        const lon = e.latlng.lng;
                        
                        if (marker) {
                            map.removeLayer(marker);
                        }
                        marker = L.marker([lat, lon])
                            .addTo(map)
                            .bindPopup('Ausgewählter Standort')
                            .openPopup();
                        
                        loadWeatherData(lat, lon, window.currentKwp1 || 9.6, window.currentKwp2 || 9.6);
                    });

                    // Standort-Initialisierung
                    if (navigator.geolocation) {
                        navigator.geolocation.getCurrentPosition(
                            position => {
                                const lat = position.coords.latitude;
                                const lon = position.coords.longitude;
                                if (marker) {
                                    map.removeLayer(marker);
                                }
                                marker = L.marker([lat, lon])
                                    .addTo(map)
                                    .bindPopup('Ihr Standort')
                                    .openPopup();
                                loadWeatherData(lat, lon, 4.8, 4.8);
                            },
                            error => {
                                // Fallback auf Berlin bei Fehler
                                const berlin = coordinates['berlin'];
                                marker = L.marker([berlin.lat, berlin.lon])
                                    .addTo(map)
                                    .bindPopup('Berlin')
                                    .openPopup();
                                loadWeatherData(berlin.lat, berlin.lon, 4.8, 4.8);
                            }
                        );
                    }

                    function handleKeyPress(event) {
                        if (event.key === 'Enter') {
                            event.preventDefault(); // Verhindert Standard-Form-Submit
                            getWeatherByCity();
                        }
                    }

                    // Globale toggleDetails Funktion
                    function toggleDetails(id) {
                        const details = document.getElementById('details-' + id);
                        if (details) {
                            const icon = details.parentElement.querySelector('.toggle-icon');
                            if (details.style.display === 'none') {
                                details.style.display = 'block';
                                if (icon) icon.style.transform = 'rotate(180deg)';
                            } else {
                                details.style.display = 'none';
                                if (icon) icon.style.transform = 'rotate(0deg)';
                            }
                        }
                    }
                </script>
            </body>
            </html>
            """;
    }

    public String getWeatherByCoordinates(double lat, double lon, String clientTime, int clientOffset) {
        try {
            // Parse client time
            LocalDateTime clientDateTime = LocalDateTime.parse(clientTime, DateTimeFormatter.ISO_DATE_TIME)
                .plusMinutes(clientOffset); // Adjust for client timezone
            
            // Use client time for calculations
            LocalDate clientDate = clientDateTime.toLocalDate();
            int clientHour = clientDateTime.getHour();
            
            String url = UriComponentsBuilder.fromUriString(WEATHER_API_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", weatherConfig.getKey())
                .queryParam("units", "metric")
                .queryParam("lang", "de")
                .build()
                .toString();

            WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);
            
            if (response != null && response.getMain() != null && response.getWeather() != null 
                && response.getWeather().length > 0) {
                String locationName = locationService.getLocationName(lat, lon);
                return String.format("""
                    <div class="category">
                        <div class="location-info" data-location="%s"></div>
                        <div class="current-weather">
                            <h3>Aktuelles Wetter</h3>
                            <table>
                                <tr>
                                    <th>🌡️ Temperatur</th>
                                    <th>Beschreibung</th>
                                    <th>☁️ Bewölkung</th>
                                    <th>Luftfeuchtigkeit</th>
                                </tr>
                                <tr>
                                    <td>%.1f°C</td>
                                    <td>%s</td>
                                    <td>%d%%</td>
                                    <td>%d%%</td>
                                </tr>
                            </table>
                        </div>
                        %s
                    </div>
                    """,
                    locationName,
                    response.getMain().getTemp(),
                    response.getWeather()[0].getDescription(),
                    response.getClouds().getAll(),
                    response.getMain().getHumidity(),
                    getForecastByCoordinates(lat, lon)
                );
            }
            return "<p>Wetterdaten konnten nicht abgerufen werden.</p>";
        } catch (Exception e) {
            return String.format("<p>Fehler beim Abrufen der Wetterdaten: %s</p>", e.getMessage());
        }
    }

    private String generateHourlyRows(double lat, LocalDate date, double avgCloudCover,
                                    double kwp1, int azimuth1, int tilt1, double efficiency1, double losses1,
                                    double kwp2, int azimuth2, int tilt2, double efficiency2, double losses2,
                                    double maxSunHeight) {  // Neuer Parameter
        // Berechne maximalen theoretischen Stundenertrag (STC)
        double maxTheoretical = calculateMaxTheoretical(kwp1, efficiency1, losses1,
                                                      kwp2, efficiency2, losses2);
        
        StringBuilder rows = new StringBuilder();
        for (int hour = 0; hour < 24; hour++) {
            double sunHeight = calculateSunHeight(lat, date, hour);
            if (sunHeight > 0) {
                double hourlyRadiation1 = calculateHourlyRadiation(lat, date, hour, avgCloudCover, azimuth1, tilt1);
                double hourlyRadiation2 = calculateHourlyRadiation(lat, date, hour, avgCloudCover, azimuth2, tilt2);
                
                double hourlyYield1 = calculateHourlyYield(hourlyRadiation1, efficiency1, losses1);
                double hourlyYield2 = calculateHourlyYield(hourlyRadiation2, efficiency2, losses2);
                
                double minHourlyRadiation = Math.min(hourlyRadiation1, hourlyRadiation2);
                double maxHourlyRadiation = Math.max(hourlyRadiation1, hourlyRadiation2);
                double avgHourlyRadiation = (hourlyRadiation1 + hourlyRadiation2) / 2.0;
                
                double hourlyTotal = (hourlyYield1 * kwp1) + (hourlyYield2 * kwp2);
                
                // Berechne den maximalen Ertrag für diese Stunde basierend auf der Sonnenhöhe
                double maxRadiationForHour = 1000.0 * Math.sin(Math.toRadians(Math.max(0, sunHeight)));
                double maxYield1ForHour = calculateHourlyYield(maxRadiationForHour, efficiency1, losses1) * kwp1;
                double maxYield2ForHour = calculateHourlyYield(maxRadiationForHour, efficiency2, losses2) * kwp2;
                double maxTotalForHour = maxYield1ForHour + maxYield2ForHour;
                
                // Berechne den maximalen Tagesertrag (wie in der Übersicht)
                double maxDayRadiation = 1000.0 * Math.sin(Math.toRadians(maxSunHeight));
                double maxDayYield1 = calculateHourlyYield(maxDayRadiation, efficiency1, losses1) * kwp1;
                double maxDayYield2 = calculateHourlyYield(maxDayRadiation, efficiency2, losses2) * kwp2;
                double maxDayTotal = maxDayYield1 + maxDayYield2;
                
                // Berechne die Balkenbreiten (0-140px)
                double currentWidth = (hourlyTotal / maxTheoretical) * 140.0;
                double maxHourWidth = (maxTotalForHour / maxTheoretical) * 140.0;
                double maxDayWidth = (maxDayTotal / maxTheoretical) * 140.0;
                
                rows.append(String.format("""
                    <tr>
                        <td>%02d:00</td>
                        <td>%.1f°</td>
                        <td>%.0f%%</td>
                        <td>%.0f - %.0f W/m² | Ø %.0f W/m²</td>
                        <td>%.3f kWh/kWp</td>
                        <td>%.2f kWh</td>
                        <td>%.3f kWh/kWp</td>
                        <td>%.2f kWh</td>
                        <td>%.2f kWh</td>
                        <td class="chart-cell">
                            <div class="bar-container">
                                <div class="bar-max" style="width: %.0fpx;"></div>
                                <div class="bar-max-day" style="width: %.0fpx;"></div>
                                <div class="bar-current" style="width: %.0fpx;"></div>
                            </div>
                        </td>
                    </tr>
                    """,
                    hour, sunHeight, avgCloudCover,
                    minHourlyRadiation, maxHourlyRadiation, avgHourlyRadiation,
                    hourlyYield1, hourlyYield1 * kwp1,
                    hourlyYield2, hourlyYield2 * kwp2,
                    hourlyTotal,
                    maxHourWidth, maxDayWidth, currentWidth   // Nur die Balkenbreiten
                ));
            }
        }
        return rows.toString();
    }

    private double calculateMinRadiation(double lat, LocalDate date, double avgCloudCover, int azimuth, int tilt) {
        double minRadiation = Double.MAX_VALUE;
        boolean hasDaylight = false;
        
        for (int hour = 0; hour < 24; hour++) {
            double sunHeight = calculateSunHeight(lat, date, hour);
            if (sunHeight > 0) {
                hasDaylight = true;
                double radiation = calculateHourlyRadiation(lat, date, hour, avgCloudCover, azimuth, tilt);
                if (radiation > 0) {
                    minRadiation = Math.min(minRadiation, radiation);
                }
            }
        }
        return hasDaylight ? minRadiation : 0;
    }

    private double getCurrentDayYield(double lat, LocalDate clientDate, double cloudCover, 
                                    int azimuth, int tilt, double kwp, double efficiency, double losses, int clientHour) {
        double yield = 0;
        for (int hour = 0; hour <= clientHour; hour++) {
            double radiation = calculateHourlyRadiation(lat, clientDate, hour, cloudCover, azimuth, tilt);
            yield += calculateHourlyYield(radiation, efficiency, losses) * kwp;
        }
        return yield;
    }

    private double getCurrentPower(double lat, LocalDate clientDate, int clientHour, 
                                 double cloudCover, int azimuth, int tilt, double kwp) {
        double radiation = getCurrentRadiation(lat, clientDate, clientHour, cloudCover, azimuth, tilt);
        return (radiation / 1000.0) * kwp * 0.96;
    }

    private double getCurrentRadiation(double lat, LocalDate clientDate, int clientHour, 
                                     double cloudCover, int azimuth, int tilt) {
        return calculateHourlyRadiation(lat, clientDate, clientHour, cloudCover, azimuth, tilt);
    }

    private String createSolarSettingsHtml() {
        return """
            <div class="settings">
                <div class="settings-grid">
                    <div class="setting-label"></div>
                    <div class="setting-field">Anlage 1</div>
                    <div class="setting-field">Anlage 2</div>

                    <div class="setting-label">Anlagengröße:</div>
                    <div class="setting-field">
                        <input type="number" id="kwp1" value="4.8" step="0.1" min="0.1"> kWp
                    </div>
                    <div class="setting-field">
                        <input type="number" id="kwp2" value="4.8" step="0.1" min="0.1"> kWp
                    </div>

                    <div class="setting-label">Ausrichtung:</div>
                    <div class="setting-field">
                        <select id="azimuth1">
                            <option value="90">Ost (90°)</option>
                            <option value="135">Südost (135°)</option>
                            <option value="180" selected>Süd (180°)</option>
                            <option value="225">Südwest (225°)</option>
                            <option value="270">West (270°)</option>
                        </select>
                    </div>
                    <div class="setting-field">
                        <select id="azimuth2">
                            <option value="90">Ost (90°)</option>
                            <option value="135">Südost (135°)</option>
                            <option value="180">Süd (180°)</option>
                            <option value="225">Südwest (225°)</option>
                            <option value="270" selected>West (270°)</option>
                        </select>
                    </div>

                    <div class="setting-label">Neigung:</div>
                    <div class="setting-field">
                        <input type="number" id="tilt1" value="18" min="0" max="90" step="1">°
                    </div>
                    <div class="setting-field">
                        <input type="number" id="tilt2" value="18" min="0" max="90" step="1">°
                    </div>

                    <div class="setting-label">Wirkungsgrad:</div>
                    <div class="setting-field">
                        <input type="number" id="efficiency1" value="20.0" min="1" max="30" step="0.1">%
                    </div>
                    <div class="setting-field">
                        <input type="number" id="efficiency2" value="20.0" min="1" max="30" step="0.1">%
                    </div>

                    <div class="setting-label">Systemverluste:</div>
                    <div class="setting-field">
                        <input type="number" id="losses1" value="14.0" min="0" max="40" step="0.1">%
                    </div>
                    <div class="setting-field">
                        <input type="number" id="losses2" value="14.0" min="0" max="40" step="0.1">%
                    </div>
                </div>

                <button onclick="updateWeather()">Aktualisieren</button>
            </div>
            """;
    }

    private double calculateSolarFactor(int azimuth, int tilt) {
        // Optimale Ausrichtung ist Süd (180°) mit 35° Neigung
        double optimalAzimuth = 180.0;
        double optimalTilt = 35.0;
        
        // Berechne Abweichung von der optimalen Ausrichtung
        double azimuthDiff = Math.abs(azimuth - optimalAzimuth);
        double tiltDiff = Math.abs(tilt - optimalTilt);
        
        // Faktor basierend auf der Abweichung (vereinfachte Berechnung)
        double azimuthFactor = 1.0 - (azimuthDiff / 180.0) * 0.3;  // max 30% Verlust durch Azimuth
        double tiltFactor = 1.0 - (tiltDiff / 90.0) * 0.2;         // max 20% Verlust durch Tilt
        
        return azimuthFactor * tiltFactor;
    }

    private double calculateMaxTheoretical(double kwp1, double efficiency1, double losses1,
                                     double kwp2, double efficiency2, double losses2) {
        // STC-Bedingungen: 1000 W/m², 25°C
        double maxRadiation = 1000.0;
        
        // Berechne maximalen Ertrag für beide Anlagen unter STC
        double maxYield1 = calculateHourlyYield(maxRadiation, efficiency1, losses1) * kwp1;
        double maxYield2 = calculateHourlyYield(maxRadiation, efficiency2, losses2) * kwp2;
        
        return maxYield1 + maxYield2;
    }

    private double calculateMaxSunHeight(double lat, LocalDate date) {
        double maxHeight = 0;
        for (int hour = 0; hour < 24; hour++) {
            double sunHeight = calculateSunHeight(lat, date, hour);
            maxHeight = Math.max(maxHeight, sunHeight);
        }
        return maxHeight;
    }
} 