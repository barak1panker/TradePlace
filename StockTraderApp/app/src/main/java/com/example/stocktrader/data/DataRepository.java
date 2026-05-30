package com.example.stocktrader.data;

import android.content.Context;

import com.example.stocktrader.models.Portfolio;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.models.User;
import com.example.stocktrader.models.UserAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton repository: serves stock data, current user and the portfolio.
 * Data is kept in-memory for the lifetime of the process.
 */
public class DataRepository {

    private static DataRepository instance;

    private final Map<String, Stock> stockMap;
    private User currentUser;
    private Portfolio portfolio;

    private DataRepository() {
        stockMap = new HashMap<>();
        seedStocks();
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) instance = new DataRepository();
        return instance;
    }

    /** Populate the simulated market with a sample of well-known stocks. */
    private void seedStocks() {
        addStock(new Stock("AAPL", "Apple Inc.", "Technology", 192.50));
        addStock(new Stock("MSFT", "Microsoft Corporation", "Technology", 415.20));
        addStock(new Stock("GOOGL", "Alphabet Inc.", "Technology", 178.30));
        addStock(new Stock("AMZN", "Amazon.com Inc.", "Consumer", 185.90));
        addStock(new Stock("META", "Meta Platforms Inc.", "Technology", 510.15));
        addStock(new Stock("TSLA", "Tesla Inc.", "Automotive", 248.75));
        addStock(new Stock("NVDA", "NVIDIA Corporation", "Technology", 920.40));
        addStock(new Stock("JPM", "JPMorgan Chase & Co.", "Financial", 198.25));
        addStock(new Stock("V", "Visa Inc.", "Financial", 275.60));
        addStock(new Stock("WMT", "Walmart Inc.", "Consumer", 62.40));
        addStock(new Stock("DIS", "The Walt Disney Company", "Entertainment", 102.55));
        addStock(new Stock("KO", "Coca-Cola Company", "Consumer", 63.10));
        addStock(new Stock("NKE", "Nike Inc.", "Consumer", 88.20));
        addStock(new Stock("PFE", "Pfizer Inc.", "Healthcare", 27.80));
        addStock(new Stock("BA", "The Boeing Company", "Industrial", 178.45));
    }

    private void addStock(Stock s) {
        stockMap.put(s.getSymbol(), s);
    }

    public List<Stock> getAllStocks() {
        List<Stock> list = new ArrayList<>(stockMap.values());
        Collections.sort(list, new Comparator<Stock>() {
            @Override
            public int compare(Stock a, Stock b) {
                return a.getSymbol().compareTo(b.getSymbol());
            }
        });
        return list;
    }

    public Stock getStock(String symbol) {
        return stockMap.get(symbol);
    }

    public List<Stock> searchStocks(String query) {
        List<Stock> results = new ArrayList<>();
        if (query == null) query = "";
        String q = query.trim().toUpperCase();
        for (Stock s : stockMap.values()) {
            if (q.isEmpty()
                    || s.getSymbol().toUpperCase().contains(q)
                    || s.getCompanyName().toUpperCase().contains(q)) {
                results.add(s);
            }
        }
        Collections.sort(results, new Comparator<Stock>() {
            @Override
            public int compare(Stock a, Stock b) {
                return a.getSymbol().compareTo(b.getSymbol());
            }
        });
        return results;
    }

    // --- Top movers ---
    public List<Stock> getTopGainers(int limit) {
        List<Stock> list = new ArrayList<>(stockMap.values());
        Collections.sort(list, new Comparator<Stock>() {
            @Override
            public int compare(Stock a, Stock b) {
                return Double.compare(b.getChangePercent(), a.getChangePercent());
            }
        });
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Stock> getTopLosers(int limit) {
        List<Stock> list = new ArrayList<>(stockMap.values());
        Collections.sort(list, new Comparator<Stock>() {
            @Override
            public int compare(Stock a, Stock b) {
                return Double.compare(a.getChangePercent(), b.getChangePercent());
            }
        });
        return list.subList(0, Math.min(limit, list.size()));
    }

    // --- User / portfolio ---
    public User getCurrentUser() { return currentUser; }
    public Portfolio getPortfolio() { return portfolio; }

    private UserAccount currentAccount;
    private Context appContext;

    public UserAccount getCurrentAccount() { return currentAccount; }

    /**
     * Log in using a verified UserAccount. Restores saved portfolio if exists.
     */
    public void loginWithAccount(Context context, UserAccount account) {
        this.appContext = context.getApplicationContext();
        this.currentAccount = account;
        this.currentUser = new User(account.getUsername(), account.getFullName(), account.getCashBalance());

        String pj = account.getPortfolioJson();
        if (pj != null && !pj.isEmpty()) {
            this.portfolio = PortfolioSerializer.fromJson(pj);
        } else {
            this.portfolio = new Portfolio();
            // First time: seed a default watchlist
            this.portfolio.addToWatchlist("AAPL");
            this.portfolio.addToWatchlist("TSLA");
            this.portfolio.addToWatchlist("NVDA");
        }
        AuthManager.getInstance(this.appContext).setLastUser(account.getUsername());
    }

    /**
     * Persist current user's cash and portfolio to the server.
     * Should be called after every buy / sell / watchlist change.
     */
    public void persistState() {
        if (appContext == null || currentAccount == null || currentUser == null || portfolio == null) return;
        currentAccount.setCashBalance(currentUser.getCashBalance());
        currentAccount.setPortfolioJson(PortfolioSerializer.toJson(portfolio));
        // Fire-and-forget save to the server. If the network is down the local
        // state remains correct; the next successful save will catch up.
        AuthManager.getInstance(appContext).saveAccountAsync(currentAccount, null);
    }

    public void logout() {
        persistState();
        if (appContext != null) {
            AuthManager.getInstance(appContext).clearLastUser();
        }
        currentUser = null;
        portfolio = null;
        currentAccount = null;
    }
}
