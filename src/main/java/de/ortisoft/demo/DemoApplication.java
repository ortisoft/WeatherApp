package de.ortisoft.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;
import de.ortisoft.demo.service.WeatherService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@RestController
@EnableCaching
public class DemoApplication {

	private final WeatherService weatherService;

	public DemoApplication(WeatherService weatherService) {
		this.weatherService = weatherService;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/")
	@ResponseBody
	public String getWetter() {
		return weatherService.getWeatherAndForecast();
	}

	@GetMapping("/weather")
	public String getWeather(
		@RequestParam double lat, 
		@RequestParam double lon,
		@RequestParam(required = false) String clientTime,
		@RequestParam(required = false, defaultValue = "0") int clientOffset,
		@RequestParam(value = "kwp1", defaultValue = "4.8") double kwp1,
		@RequestParam(value = "azimuth1", defaultValue = "90") int azimuth1,
		@RequestParam(value = "tilt1", defaultValue = "18") int tilt1,
		@RequestParam(value = "efficiency1", defaultValue = "20.0") double efficiency1,
		@RequestParam(value = "losses1", defaultValue = "14.0") double losses1,
		@RequestParam(value = "kwp2", defaultValue = "4.8") double kwp2,
		@RequestParam(value = "azimuth2", defaultValue = "270") int azimuth2,
		@RequestParam(value = "tilt2", defaultValue = "18") int tilt2,
		@RequestParam(value = "efficiency2", defaultValue = "20.0") double efficiency2,
		@RequestParam(value = "losses2", defaultValue = "14.0") double losses2
	) {
		if (clientTime == null) {
			// Fallback auf Server-Zeit wenn keine Client-Zeit übergeben wurde
			clientTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
		}

		StringBuilder response = new StringBuilder();
		
		// Füge Wetterdaten hinzu
		response.append(weatherService.getWeatherByCoordinates(lat, lon, clientTime, clientOffset));
		
		// Füge Solardaten hinzu
		response.append(weatherService.getSolarData(lat, lon, 
			kwp1, azimuth1, tilt1, efficiency1, losses1,
			kwp2, azimuth2, tilt2, efficiency2, losses2));
		
		return response.toString();
	}

	@GetMapping("/solar")
	public String getSolarData(@RequestParam double lat, @RequestParam double lon,
							 @RequestParam double kwp1, @RequestParam int azimuth1, @RequestParam int tilt1,
							 @RequestParam double efficiency1, @RequestParam double losses1,
							 @RequestParam double kwp2, @RequestParam int azimuth2, @RequestParam int tilt2,
							 @RequestParam double efficiency2, @RequestParam double losses2) {
		return weatherService.getSolarData(lat, lon, 
			kwp1, azimuth1, tilt1, efficiency1, losses1,
			kwp2, azimuth2, tilt2, efficiency2, losses2);
	}

}
