#!/bin/bash

# Verzeichnis für Logs erstellen
mkdir -p /var/log/weather-app

# Java-Optionen für Produktion
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Umgebungsvariablen
export WEATHER_API_KEY=your_api_key_here

# Anwendung starten
java $JAVA_OPTS \
  -Dspring.profiles.active=prod \
  -Dserver.port=8080 \
  -jar path/to/app.jar 