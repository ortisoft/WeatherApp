package de.ortisoft.demo.service;

import de.ortisoft.demo.config.WeatherConfig;
import de.ortisoft.demo.model.GeoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LocationService {
    private static final String GEO_API_URL = "http://api.openweathermap.org/geo/1.0/reverse";
    
    private final WeatherConfig weatherConfig;
    private final RestTemplate restTemplate;

    public LocationService(WeatherConfig weatherConfig) {
        this.weatherConfig = weatherConfig;
        this.restTemplate = new RestTemplate();
    }

    public String getLocationInfo(double lat, double lon) {
        String url = UriComponentsBuilder.fromUriString(GEO_API_URL)
            .queryParam("lat", lat)
            .queryParam("lon", lon)
            .queryParam("limit", 1)
            .queryParam("appid", weatherConfig.getKey())
            .build()
            .toString();

        try {
            GeoResponse[] response = restTemplate.getForObject(url, GeoResponse[].class);
            if (response != null && response.length > 0) {
                return response[0].getName();
            }
        } catch (Exception e) {
            // Log error and return fallback
        }
        return String.format("%.6f°, %.6f°", lat, lon);
    }

    public String getLocationName(double lat, double lon) {
        try {
            String url = UriComponentsBuilder.fromUriString(GEO_API_URL)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("limit", 1)
                .queryParam("appid", weatherConfig.getKey())
                .build()
                .toString();

            GeoResponse[] response = restTemplate.getForObject(url, GeoResponse[].class);
            
            if (response != null && response.length > 0) {
                return response[0].getName();
            }
            return "Unbekannter Ort";
        } catch (Exception e) {
            return "Unbekannter Ort";
        }
    }
} 