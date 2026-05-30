package com.example.stocktrader.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A transaction record: buy or sell of a stock, with timestamp.
 */
public class Transaction implements Serializable {

    public enum Type { BUY, SELL }

    private String symbol;
    private Type type;
    private int quantity;
    private double pricePerShare;
    private long timestamp;
    private double realizedPL; // for sell only

    public Transaction(String symbol, Type type, int quantity, double pricePerShare) {
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.timestamp = System.currentTimeMillis();
        this.realizedPL = 0;
    }

    public String getSymbol() { return symbol; }
    public Type getType() { return type; }
    public int getQuantity() { return quantity; }
    public double getPricePerShare() { return pricePerShare; }
    public long getTimestamp() { return timestamp; }
    public double getRealizedPL() { return realizedPL; }

    public void setRealizedPL(double pl) { this.realizedPL = pl; }

    /** Used by the serializer to restore the original timestamp. */
    public void setTimestamp(long ts) { this.timestamp = ts; }

    public double getTotalValue() {
        return quantity * pricePerShare;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
