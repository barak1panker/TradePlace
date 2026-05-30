package com.example.stocktrader.models;

import java.io.Serializable;

/**
 * Represents a single holding (position) in the user's portfolio.
 * A holding has an average cost (weighted) and a current quantity.
 */
public class Holding implements Serializable {

    private String symbol;
    private int quantity;
    private double averageCost; // weighted average buy price

    public Holding(String symbol, int quantity, double averageCost) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageCost = averageCost;
    }

    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public double getAverageCost() { return averageCost; }

    /** Add bought shares - recompute weighted average. */
    public void addBuy(int qty, double price) {
        double totalCost = averageCost * quantity + price * qty;
        quantity += qty;
        averageCost = quantity > 0 ? totalCost / quantity : 0;
    }

    /** Reduce shares on sell. Avg cost unchanged on sells. */
    public void reduceOnSell(int qty) {
        quantity -= qty;
        if (quantity < 0) quantity = 0;
    }

    public double marketValue(double currentPrice) {
        return quantity * currentPrice;
    }

    public double costBasis() {
        return quantity * averageCost;
    }

    public double unrealizedPL(double currentPrice) {
        return marketValue(currentPrice) - costBasis();
    }

    public double unrealizedPLPercent(double currentPrice) {
        double cost = costBasis();
        if (cost == 0) return 0;
        return (unrealizedPL(currentPrice) / cost) * 100.0;
    }
}
