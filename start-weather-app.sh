#!/bin/bash

# Verzeichnis für Logs erstellen
mkdir -p /var/log/weather-app

# Java-Optionen für Produktion
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Umgebungsvariablen
export WEATHER_API_KEY=160fc664970e11e8772f59bc9489728f

# Anwendung starten
java $JAVA_OPTS \
  -Dspring.profiles.active=prod \
  -Dserver.port=8080 \
  -jar /var/www/vhosts/system/weather.intellibits.de/app/demo-0.0.1-SNAPSHOT.jar 