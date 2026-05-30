package com.example.stocktrader.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.stocktrader.models.NewsArticle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Network client for Alpha Vantage (stock data) and NewsAPI.org (news).
 * All HTTP work happens on a background executor; callers get the result
 * on the main thread via Callback<T>.
 *
 * Both APIs are rate-limited on the free tier, so we cache results in
 * memory for the lifetime of the process.
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String ALPHA_VANTAGE_KEY = "9ADA80LVP8NVH5WS";
    private static final String NEWS_API_KEY = "212d513a340541ff9990bb0bac456371";

    // Cache TTLs in millis.
    private static final long PRICE_CACHE_TTL = 15 * 60 * 1000L; // 15 min
    private static final long NEWS_CACHE_TTL  = 30 * 60 * 1000L; // 30 min

    private static ApiClient instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    private final Map<String, CachedHistory> priceCache = new HashMap<>();
    private final Map<String, CachedNews> newsCache = new HashMap<>();

    private ApiClient() {}

    public static synchronized ApiClient getInstance() {
        if (instance == null) instance = new ApiClient();
        return instance;
    }

    /** Callback contract - delivered on the main thread. */
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    /** Point in the historical price series. */
    public static class PricePoint {
        public final String date;
        public final double close;

        public PricePoint(String date, double close) {
            this.date = date;
            this.close = close;
        }
    }

    private static class CachedHistory {
        final long timestamp;
        final List<PricePoint> data;
        CachedHistory(List<PricePoint> data) {
            this.timestamp = System.currentTimeMillis();
            this.data = data;
        }
    }

    private static class CachedNews {
        final long timestamp;
        final List<NewsArticle> data;
        CachedNews(List<NewsArticle> data) {
            this.timestamp = System.currentTimeMillis();
            this.data = data;
        }
    }

    // =================================================================
    // Public methods
    // =================================================================

    /**
     * Fetch the most recent 20 daily closes from Alpha Vantage.
     * Returns chronological order (oldest -> newest), like the Python version.
     */
    public void fetchStockHistory(final String symbol, final Callback<List<PricePoint>> cb) {
        // Check cache first
        CachedHistory cached = priceCache.get(symbol);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < PRICE_CACHE_TTL) {
            cb.onSuccess(cached.data);
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://www.alphavantage.co/query"
                            + "?function=TIME_SERIES_DAILY"
                            + "&symbol=" + URLEncoder.encode(symbol, "UTF-8")
                            + "&apikey=" + ALPHA_VANTAGE_KEY;
                    String body = httpGet(url);
                    JSONObject root = new JSONObject(body);

                    // Alpha Vantage uses several error shapes
                    if (root.has("Note")) {
                        deliverError(cb, "מגבלת קריאות API הושגה. נסה שוב בעוד דקה");
                        return;
                    }
                    if (root.has("Error Message")) {
                        deliverError(cb, "המניה לא נמצאה ב-API");
                        return;
                    }
                    if (!root.has("Time Series (Daily)")) {
                        deliverError(cb, "אין נתונים זמינים עבור " + symbol);
                        return;
                    }

                    JSONObject series = root.getJSONObject("Time Series (Daily)");
                    List<String> dates = new ArrayList<>();
                    java.util.Iterator<String> it = series.keys();
                    while (it.hasNext()) dates.add(it.next());
                    Collections.sort(dates, Collections.reverseOrder()); // newest first
                    if (dates.size() > 20) dates = dates.subList(0, 20);
                    Collections.reverse(dates); // oldest -> newest, like the Python sample

                    List<PricePoint> result = new ArrayList<>();
                    for (String date : dates) {
                        JSONObject point = series.getJSONObject(date);
                        double close = point.getDouble("4. close");
                        result.add(new PricePoint(date, close));
                    }
                    priceCache.put(symbol, new CachedHistory(result));
                    deliverSuccess(cb, result);
                } catch (Exception e) {
                    Log.e(TAG, "fetchStockHistory failed", e);
                    deliverError(cb, "תקלת רשת: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Fetch up to 5 most relevant English news articles about a symbol.
     */
    public void fetchNews(final String query, final Callback<List<NewsArticle>> cb) {
        CachedNews cached = newsCache.get(query);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < NEWS_CACHE_TTL) {
            cb.onSuccess(cached.data);
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://newsapi.org/v2/everything"
                            + "?apiKey=" + NEWS_API_KEY
                            + "&q=" + URLEncoder.encode(query, "UTF-8")
                            + "&sortBy=relevancy"
                            + "&language=en"
                            + "&pageSize=5";
                    String body = httpGet(url);
                    JSONObject root = new JSONObject(body);
                    String status = root.optString("status", "");
                    if (!"ok".equals(status)) {
                        deliverError(cb, "שגיאה בקבלת חדשות: " + root.optString("message", ""));
                        return;
                    }
                    JSONArray articles = root.optJSONArray("articles");
                    List<NewsArticle> result = new ArrayList<>();
                    if (articles != null) {
                        int count = Math.min(5, articles.length());
                        for (int i = 0; i < count; i++) {
                            JSONObject a = articles.getJSONObject(i);
                            String title = a.optString("title", "");
                            String desc = a.optString("description", "");
                            String articleUrl = a.optString("url", "");
                            String published = a.optString("publishedAt", "");
                            String image = a.optString("urlToImage", "");
                            String sourceName = "";
                            JSONObject src = a.optJSONObject("source");
                            if (src != null) sourceName = src.optString("name", "");
                            result.add(new NewsArticle(title, desc, articleUrl, sourceName, published, image));
                        }
                    }
                    newsCache.put(query, new CachedNews(result));
                    deliverSuccess(cb, result);
                } catch (Exception e) {
                    Log.e(TAG, "fetchNews failed", e);
                    deliverError(cb, "תקלת רשת: " + e.getMessage());
                }
            }
        });
    }

    // =================================================================
    // Helpers
    // =================================================================

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "StockTraderApp/1.0");
            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) throw new Exception("Empty response (code " + code + ")");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private <T> void deliverSuccess(final Callback<T> cb, final T data) {
        main.post(new Runnable() {
            @Override public void run() { cb.onSuccess(data); }
        });
    }

    private <T> void deliverError(final Callback<T> cb, final String msg) {
        main.post(new Runnable() {
            @Override public void run() { cb.onError(msg); }
        });
    }
}
