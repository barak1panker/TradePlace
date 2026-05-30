package com.example.stocktrader.models;

import com.example.stocktrader.data.DataRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Portfolio holds the user's positions and transaction history.
 * It is the central model behind the buy/sell logic and the analytics.
 */
public class Portfolio implements Serializable {

    private Map<String, Holding> holdings;
    private List<Transaction> transactions;
    private List<String> watchlist;
    private double totalRealizedPL;

    public Portfolio() {
        this.holdings = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.watchlist = new ArrayList<>();
        this.totalRealizedPL = 0;
    }

    public Map<String, Holding> getHoldings() { return holdings; }
    public List<Transaction> getTransactions() { return transactions; }
    public List<String> getWatchlist() { return watchlist; }
    public double getTotalRealizedPL() { return totalRealizedPL; }

    /** Used by the serializer when restoring a saved portfolio. */
    public void setRestoredRealizedPL(double v) { this.totalRealizedPL = v; }

    public Holding getHolding(String symbol) {
        return holdings.get(symbol);
    }

    /**
     * Execute a buy. Returns null on success, error message otherwise.
     */
    public String executeBuy(User user, Stock stock, int quantity) {
        if (quantity <= 0) return "כמות חייבת להיות גדולה מאפס";
        double cost = stock.getCurrentPrice() * quantity;
        if (user.getCashBalance() < cost) {
            return "אין מספיק כסף בחשבון. נדרש: " + String.format("%.2f", cost);
        }
        if (!user.withdraw(cost)) return "פעולת חיוב נכשלה";

        Holding existing = holdings.get(stock.getSymbol());
        if (existing == null) {
            holdings.put(stock.getSymbol(), new Holding(stock.getSymbol(), quantity, stock.getCurrentPrice()));
        } else {
            existing.addBuy(quantity, stock.getCurrentPrice());
        }

        Transaction tx = new Transaction(stock.getSymbol(), Transaction.Type.BUY, quantity, stock.getCurrentPrice());
        transactions.add(tx);
        stock.addVolume(quantity);
        return null; // success
    }

    /**
     * Execute a sell. Returns null on success, error message otherwise.
     */
    public String executeSell(User user, Stock stock, int quantity) {
        if (quantity <= 0) return "כמות חייבת להיות גדולה מאפס";
        Holding existing = holdings.get(stock.getSymbol());
        if (existing == null || existing.getQuantity() < quantity) {
            return "אין מספיק מניות בתיק למכירה";
        }
        double proceeds = stock.getCurrentPrice() * quantity;
        double realized = (stock.getCurrentPrice() - existing.getAverageCost()) * quantity;
        existing.reduceOnSell(quantity);
        if (existing.getQuantity() == 0) {
            holdings.remove(stock.getSymbol());
        }
        user.deposit(proceeds);
        totalRealizedPL += realized;

        Transaction tx = new Transaction(stock.getSymbol(), Transaction.Type.SELL, quantity, stock.getCurrentPrice());
        tx.setRealizedPL(realized);
        transactions.add(tx);
        stock.addVolume(quantity);
        return null;
    }

    public double getTotalMarketValue(DataRepository repo) {
        double total = 0;
        for (Holding h : holdings.values()) {
            Stock s = repo.getStock(h.getSymbol());
            if (s != null) total += h.marketValue(s.getCurrentPrice());
        }
        return total;
    }

    public double getTotalUnrealizedPL(DataRepository repo) {
        double total = 0;
        for (Holding h : holdings.values()) {
            Stock s = repo.getStock(h.getSymbol());
            if (s != null) total += h.unrealizedPL(s.getCurrentPrice());
        }
        return total;
    }

    public double getTotalCostBasis() {
        double total = 0;
        for (Holding h : holdings.values()) total += h.costBasis();
        return total;
    }

    // --- Watchlist ---
    public boolean addToWatchlist(String symbol) {
        if (!watchlist.contains(symbol)) {
            watchlist.add(symbol);
            return true;
        }
        return false;
    }

    public boolean removeFromWatchlist(String symbol) {
        return watchlist.remove(symbol);
    }

    public boolean isInWatchlist(String symbol) {
        return watchlist.contains(symbol);
    }
}
