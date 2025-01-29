package de.ortisoft.demo.service;

import de.ortisoft.demo.config.WeatherConfig;
import de.ortisoft.demo.model.WeatherResponse;
import de.ortisoft.demo.model.Main;
import de.ortisoft.demo.model.Weather;
import de.ortisoft.demo.model.Clouds;
import de.ortisoft.demo.model.ForecastResponse;
import de.ortisoft.demo.model.ForecastItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.contains;

@SpringBootTest
class WeatherServiceTest {

    @Value("${weather.api.key}")
    private String apiKey;

    @MockBean
    private WeatherConfig weatherConfig;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private LocationService locationService;

    @Autowired
    private WeatherService weatherService;

    @Test
    void shouldGetWeatherForBerlin() {
        // Arrange
        WeatherResponse mockResponse = createMockWeatherResponse();
        String clientTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        when(weatherConfig.getKey()).thenReturn(apiKey);
        when(locationService.getLocationName(52.520008, 13.404954)).thenReturn("Berlin");
        when(restTemplate.getForObject(
            contains("api.openweathermap.org/data/2.5/weather?lat=52.520008&lon=13.404954&appid=" + apiKey + "&units=metric&lang=de"), 
            eq(WeatherResponse.class)
        )).thenReturn(mockResponse);

        // Act
        String result = weatherService.getWeatherByCoordinates(52.520008, 13.404954, clientTime, 0);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Aktuelles Wetter"));
        assertFalse(result.contains("Fehler beim Abrufen der Wetterdaten"));
    }

    @Test
    void shouldCalculateSolarData() {
        // Arrange
        WeatherResponse mockWeather = createMockWeatherResponse();
        ForecastResponse mockForecast = createMockForecastResponse();

        when(weatherConfig.getKey()).thenReturn(apiKey);
        when(restTemplate.getForObject(
            contains("api.openweathermap.org/data/2.5/weather?lat=52.520008&lon=13.404954&appid=" + apiKey + "&units=metric&lang=de"), 
            eq(WeatherResponse.class)
        )).thenReturn(mockWeather);
        when(restTemplate.getForObject(
            contains("api.openweathermap.org/data/2.5/forecast?lat=52.520008&lon=13.404954&appid=" + apiKey + "&units=metric&lang=de"), 
            eq(ForecastResponse.class)
        )).thenReturn(mockForecast);

        // Act
        String result = weatherService.getSolarData(52.520008, 13.404954, 4.8, 90, 18, 4.8, 270, 18);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("kWh"), "Should contain 'kWh'");
        assertTrue(result.contains("Anlage"), "Should contain 'Anlage'");
        assertTrue(result.contains("W/m²"), "Should contain 'W/m²'");
        assertFalse(result.contains("Fehler"), "Should not contain 'Fehler'");
    }

    @Test
    void shouldCalculateHourlyYieldWithDefaultParameters() {
        // Arrange
        WeatherService service = new WeatherService(weatherConfig, locationService);
        double[] testRadiations = {0, 100, 500, 1000};  // W/m²
        
        // Standardwerte für beide Anlagen
        double efficiency1 = 20.0;  // 20%
        double losses1 = 14.0;      // 14%
        double efficiency2 = 20.0;  // 20%
        double losses2 = 14.0;      // 14%
        
        // Expected results (vorher berechnet)
        double[] expectedYields = {
            0.000,      // Bei 0 W/m²
            0.017,      // Bei 100 W/m²
            0.086,      // Bei 500 W/m²
            0.172       // Bei 1000 W/m²
        };
        
        // Act & Assert
        for (int i = 0; i < testRadiations.length; i++) {
            double yield1 = service.calculateHourlyYield(testRadiations[i], efficiency1, losses1);
            double yield2 = service.calculateHourlyYield(testRadiations[i], efficiency2, losses2);
            
            assertEquals(expectedYields[i], yield1, 0.001, 
                String.format("Yield1 bei %f W/m² sollte %f sein", testRadiations[i], expectedYields[i]));
            assertEquals(expectedYields[i], yield2, 0.001, 
                String.format("Yield2 bei %f W/m² sollte %f sein", testRadiations[i], expectedYields[i]));
        }
    }

    private WeatherResponse createMockWeatherResponse() {
        WeatherResponse response = new WeatherResponse();
        
        Main main = new Main();
        main.setTemp(20.5);
        main.setHumidity(65);
        response.setMain(main);

        Weather weather = new Weather();
        weather.setDescription("Leicht bewölkt");
        response.setWeather(new Weather[]{weather});

        Clouds clouds = new Clouds();
        clouds.setAll(25);  // 25% Bewölkung für realistische Solarberechnungen
        response.setClouds(clouds);

        return response;
    }

    private ForecastResponse createMockForecastResponse() {
        ForecastResponse response = new ForecastResponse();
        List<ForecastItem> forecastList = new ArrayList<>();
        
        // Erstelle ein paar Forecast-Items für den aktuellen Tag
        LocalDateTime now = LocalDateTime.now();
        for (int hour = 6; hour <= 18; hour++) {
            ForecastItem item = new ForecastItem();
            
            Main main = new Main();
            main.setTemp(20.0);
            main.setHumidity(65);
            item.setMain(main);

            Weather weather = new Weather();
            weather.setDescription("Leicht bewölkt");
            item.setWeather(new Weather[]{weather});

            Clouds clouds = new Clouds();
            clouds.setAll(25);
            item.setClouds(clouds);

            item.setDt_txt(now.withHour(hour).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            forecastList.add(item);
        }
        
        response.setList(forecastList);
        return response;
    }
} 