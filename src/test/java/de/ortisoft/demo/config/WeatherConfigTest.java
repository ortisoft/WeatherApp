package de.ortisoft.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WeatherConfigTest {

    @Autowired
    private WeatherConfig weatherConfig;

    @Test
    void shouldLoadApiKey() {
        assertNotNull(weatherConfig.getKey());
        assertFalse(weatherConfig.getKey().isEmpty());
    }
} 