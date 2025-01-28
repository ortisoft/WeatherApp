# Weather App

A Spring Boot application for displaying weather data and solar yield forecasts.

Coded completely by AI with human guidance (with Cursor AI)

## Development Process

This application was developed through an AI-human collaboration using Cursor AI. The development process was unique:

1. **AI-Driven Development**: As an AI (Claude 3.5 Sonnet), I wrote all the code based on human requirements and feedback
2. **Human Guidance**: A human developer guided the process by:
   - Providing initial requirements
   - Testing the code
   - Suggesting improvements
   - Making architectural decisions
3. **Iterative Approach**: The application evolved through multiple iterations of:
   - AI generating code
   - Human testing and feedback
   - AI making adjustments and improvements

This project demonstrates how AI can effectively write complex applications while working in tandem with human developers.

## Architecture

The application follows a classic Spring Boot architecture with clear separation of concerns:

- **Controllers**: `DemoApplication` handles HTTP requests and serves the web interface
- **Services**: 
  - `WeatherService`: Core business logic for weather data and solar calculations
  - `LocationService`: Handles geolocation and city information
- **Configuration**: 
  - `WeatherConfig`: Manages API keys and application settings
  - Properties files for environment-specific configuration
- **Models**: 
  - Data classes for weather, forecast, and location information
  - Clean separation of API responses and internal data structures
- **External APIs**:
  - OpenWeatherMap API for weather data
  - Leaflet.js for interactive maps
  - Dark mode support through CSS media queries

The frontend is embedded in the backend and uses vanilla JavaScript with modern features for a responsive user interface.

## Setup

1. Copy `src/main/resources/application-example.properties` to `src/main/resources/application.properties`
2. Enter your OpenWeatherMap API key in `application.properties`
3. Start the application with `./mvnw spring-boot:run`

## Features

- Weather display for German cities
- Solar yield forecast
- Dark mode support
- Interactive map 