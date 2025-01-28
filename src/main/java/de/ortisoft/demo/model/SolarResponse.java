package de.ortisoft.demo.model;

public class SolarResponse {
    private double dailyYield1;
    private double dailyYield2;
    private double totalYield;
    private String date;
    private String dayName;
    private String dateStr;
    private double avgCloudCover;
    private double maxRadiation;
    private double avgRadiation;

    // Getter und Setter
    public double getDailyYield1() { return dailyYield1; }
    public void setDailyYield1(double dailyYield1) { this.dailyYield1 = dailyYield1; }
    public double getDailyYield2() { return dailyYield2; }
    public void setDailyYield2(double dailyYield2) { this.dailyYield2 = dailyYield2; }
    public double getTotalYield() { return totalYield; }
    public void setTotalYield(double totalYield) { this.totalYield = totalYield; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }
    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }
    public double getAvgCloudCover() { return avgCloudCover; }
    public void setAvgCloudCover(double avgCloudCover) { this.avgCloudCover = avgCloudCover; }
    public double getMaxRadiation() { return maxRadiation; }
    public void setMaxRadiation(double maxRadiation) { this.maxRadiation = maxRadiation; }
    public double getAvgRadiation() { return avgRadiation; }
    public void setAvgRadiation(double avgRadiation) { this.avgRadiation = avgRadiation; }
} 