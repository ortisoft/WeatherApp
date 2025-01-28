package de.ortisoft.demo.service;

import de.ortisoft.demo.config.WeatherConfig;
import de.ortisoft.demo.model.GeoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class LocationServiceTest {

    @Value("${weather.api.key}")
    private String apiKey;

    @MockBean
    private WeatherConfig weatherConfig;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private LocationService locationService;

    @Test
    void shouldGetLocationNameForBerlin() {
        // Arrange
        GeoResponse mockResponse = new GeoResponse();
        mockResponse.setName("Berlin");
        GeoResponse[] responses = new GeoResponse[]{mockResponse};

        when(weatherConfig.getKey()).thenReturn(apiKey);  // API-Key aus Properties
        when(restTemplate.getForObject(anyString(), eq(GeoResponse[].class)))
            .thenReturn(responses);

        // Act
        String location = locationService.getLocationName(52.520008, 13.404954);

        // Assert
        assertNotNull(location);
        assertEquals("Berlin", location);
    }

    @Test
    void shouldHandleInvalidCoordinates() {
        // Arrange
        when(weatherConfig.getKey()).thenReturn("test_api_key");
        when(restTemplate.getForObject(anyString(), eq(GeoResponse[].class)))
            .thenReturn(new GeoResponse[0]);

        // Act
        String location = locationService.getLocationName(0.0, 0.0);

        // Assert
        assertEquals("Unbekannter Ort", location);
    }

    @Test
    void shouldGetLocationNameForRostock() {
        // Arrange
        GeoResponse mockResponse = new GeoResponse();
        mockResponse.setName("Rostock");
        GeoResponse[] responses = new GeoResponse[]{mockResponse};

        when(weatherConfig.getKey()).thenReturn("test_api_key");
        when(restTemplate.getForObject(anyString(), eq(GeoResponse[].class)))
            .thenReturn(responses);

        // Act
        String location = locationService.getLocationName(54.083333, 12.133333);

        // Assert
        assertNotNull(location);
        assertEquals("Rostock", location);
    }
} 