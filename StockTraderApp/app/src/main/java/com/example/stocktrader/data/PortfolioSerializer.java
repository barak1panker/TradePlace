package com.example.stocktrader.data;

import com.example.stocktrader.models.Holding;
import com.example.stocktrader.models.Portfolio;
import com.example.stocktrader.models.Transaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Converts a Portfolio to JSON and back.
 * Stored as a single string inside the UserAccount.
 */
public class PortfolioSerializer {

    private PortfolioSerializer() {}

    public static String toJson(Portfolio p) {
        if (p == null) return null;
        try {
            JSONObject root = new JSONObject();
            root.put("totalRealizedPL", p.getTotalRealizedPL());

            JSONArray holdings = new JSONArray();
            for (Holding h : p.getHoldings().values()) {
                JSONObject ho = new JSONObject();
                ho.put("symbol", h.getSymbol());
                ho.put("quantity", h.getQuantity());
                ho.put("averageCost", h.getAverageCost());
                holdings.put(ho);
            }
            root.put("holdings", holdings);

            JSONArray txs = new JSONArray();
            for (Transaction t : p.getTransactions()) {
                JSONObject to = new JSONObject();
                to.put("symbol", t.getSymbol());
                to.put("type", t.getType().name());
                to.put("quantity", t.getQuantity());
                to.put("pricePerShare", t.getPricePerShare());
                to.put("timestamp", t.getTimestamp());
                to.put("realizedPL", t.getRealizedPL());
                txs.put(to);
            }
            root.put("transactions", txs);

            JSONArray watch = new JSONArray();
            for (String sym : p.getWatchlist()) watch.put(sym);
            root.put("watchlist", watch);

            return root.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public static Portfolio fromJson(String json) {
        Portfolio p = new Portfolio();
        if (json == null || json.isEmpty()) return p;
        try {
            JSONObject root = new JSONObject(json);

            JSONArray holdings = root.optJSONArray("holdings");
            if (holdings != null) {
                for (int i = 0; i < holdings.length(); i++) {
                    JSONObject ho = holdings.getJSONObject(i);
                    Holding h = new Holding(
                            ho.getString("symbol"),
                            ho.getInt("quantity"),
                            ho.getDouble("averageCost"));
                    p.getHoldings().put(h.getSymbol(), h);
                }
            }

            JSONArray txs = root.optJSONArray("transactions");
            if (txs != null) {
                for (int i = 0; i < txs.length(); i++) {
                    JSONObject to = txs.getJSONObject(i);
                    Transaction t = new Transaction(
                            to.getString("symbol"),
                            Transaction.Type.valueOf(to.getString("type")),
                            to.getInt("quantity"),
                            to.getDouble("pricePerShare"));
                    t.setRealizedPL(to.optDouble("realizedPL", 0));
                    t.setTimestamp(to.optLong("timestamp", System.currentTimeMillis()));
                    p.getTransactions().add(t);
                }
            }

            // For watchlist:
            JSONArray watch = root.optJSONArray("watchlist");
            if (watch != null) {
                for (int i = 0; i < watch.length(); i++) {
                    p.addToWatchlist(watch.getString(i));
                }
            }

            // total realized PL via reflection-free path - use field updates via Portfolio API.
            double realized = root.optDouble("totalRealizedPL", 0);
            // Apply by replaying transactions effect already gives realizedPL per tx.
            // We instead expose a setter through restoring directly:
            p.setRestoredRealizedPL(realized);

        } catch (JSONException ignored) {
            // return whatever we managed to parse
        }
        return p;
    }
}
