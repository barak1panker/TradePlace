package com.example.stocktrader.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock model - represents a tradable stock in the market.
 * Holds price info, history and simulated market behavior.
 */
public class Stock implements Serializable {

    private String symbol;          // e.g. AAPL
    private String companyName;     // e.g. Apple Inc.
    private String sector;          // e.g. Technology
    private double currentPrice;
    private double openPrice;
    private double previousClose;
    private double dayHigh;
    private double dayLow;
    private long volume;
    private List<Double> priceHistory; // recent points for chart

    public Stock(String symbol, String companyName, String sector, double currentPrice) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
        this.currentPrice = currentPrice;
        this.openPrice = currentPrice;
        this.previousClose = currentPrice;
        this.dayHigh = currentPrice;
        this.dayLow = currentPrice;
        this.volume = 0;
        this.priceHistory = new ArrayList<>();
        this.priceHistory.add(currentPrice);
    }

    // --- Getters ---
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public String getSector() { return sector; }
    public double getCurrentPrice() { return currentPrice; }
    public double getOpenPrice() { return openPrice; }
    public double getPreviousClose() { return previousClose; }
    public double getDayHigh() { return dayHigh; }
    public double getDayLow() { return dayLow; }
    public long getVolume() { return volume; }
    public List<Double> getPriceHistory() { return priceHistory; }

    // --- Setters / mutators ---
    public void setCurrentPrice(double newPrice) {
        this.currentPrice = newPrice;
        if (newPrice > dayHigh) dayHigh = newPrice;
        if (newPrice < dayLow) dayLow = newPrice;
        priceHistory.add(newPrice);
        // Keep history bounded
        if (priceHistory.size() > 60) {
            priceHistory.remove(0);
        }
    }

    public void addVolume(long qty) {
        this.volume += qty;
    }

    // --- Derived values ---
    public double getChange() {
        return currentPrice - previousClose;
    }

    public double getChangePercent() {
        if (previousClose == 0) return 0;
        return ((currentPrice - previousClose) / previousClose) * 100.0;
    }

    public boolean isUp() {
        return getChange() >= 0;
    }
}
