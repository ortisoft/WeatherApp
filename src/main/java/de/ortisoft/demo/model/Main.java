package de.ortisoft.demo.model;

public class Main {
    private double temp;
    private int humidity;  // Luftfeuchtigkeit in Prozent

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public int getHumidity() {  // Dies ist der korrekte Getter-Name
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }
} 