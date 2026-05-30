package com.example.stocktrader.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.stocktrader.models.UserAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Network client for the auth/account backend.
 * All requests run on a background thread and deliver results to the main thread.
 *
 * BASE_URL is set to 10.0.2.2 which is the Android emulator's loopback to the host
 * machine. On a real device, change it to the LAN IP of the machine running the server.
 */
public class ApiAuthClient {

    private static final String TAG = "ApiAuthClient";

    /**
     * Server base URL.
     * - 10.0.2.2  → Android emulator → host machine's localhost
     * - http://192.168.x.x:3000  → real device on the same WiFi as the server
     */
    public static final String BASE_URL = "http://10.0.2.2:3000";

    private static ApiAuthClient instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    private ApiAuthClient() {}

    public static synchronized ApiAuthClient getInstance() {
        if (instance == null) instance = new ApiAuthClient();
        return instance;
    }

    /** Generic callback delivered on the main thread. */
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    // =================================================================
    // Public API
    // =================================================================

    /** Create a new account by sending plain username/password to the server. */
    public void register(final String username, final String fullName,
                         final String password, final Callback<UserAccount> cb) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("username", username);
                    body.put("fullName", fullName);
                    body.put("password", password);
                    JSONObject res = httpPost("/api/register", body);
                    handleAccountResponse(res, cb);
                } catch (Exception e) {
                    deliverError(cb, "תקלת רשת: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Migrate a local account to the server: we already have the password HASH
     * (computed locally before this migration) and skip the local password rules
     * on the server side.
     */
    public void registerWithHash(final String username, final String fullName,
                                 final String passwordHash, final Callback<UserAccount> cb) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("username", username);
                    body.put("fullName", fullName);
                    body.put("passwordHash", passwordHash);
                    JSONObject res = httpPost("/api/register", body);
                    handleAccountResponse(res, cb);
                } catch (Exception e) {
                    deliverError(cb, "תקלת רשת: " + e.getMessage());
                }
            }
        });
    }

    /** Log in - sends the plain password; server hashes and compares. */
    public void login(final String username, final String password,
                      final Callback<UserAccount> cb) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("username", username);
                    body.put("password", password);
                    JSONObject res = httpPost("/api/login", body);
                    handleAccountResponse(res, cb);
                } catch (Exception e) {
                    deliverError(cb, "תקלת רשת: " + e.getMessage());
                }
            }
        });
    }

    /** Refresh a single account from the server (cash + portfolio). */
    public void fetchAccount(final String username, final Callback<UserAccount> cb) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/api/account/" + URLEncoder.encode(username, "UTF-8");
                    JSONObject res = httpGet(path);
                    handleAccountResponse(res, cb);
                } catch (Exception e) {
                    deliverError(cb, "תקלת רשת: " + e.getMessage());
                }
            }
        });
    }

    /** Save the user's cash balance + portfolio JSON to the server. */
    public void saveAccount(final String username, final double cashBalance,
                            final String portfolioJson, final Callback<UserAccount> cb) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("cashBalance", cashBalance);
                    if (portfolioJson != null) body.put("portfolioJson", portfolioJson);
                    String path = "/api/account/" + URLEncoder.encode(username, "UTF-8");
                    JSONObject res = httpPut(path, body);
                    handleAccountResponse(res, cb);
                } catch (Exception e) {
                    deliverError(cb, "תקלת רשת: " + e.getMessage());
                }
            }
        });
    }

    // =================================================================
    // Response parsing
    // =================================================================

    private void handleAccountResponse(JSONObject res, Callback<UserAccount> cb) throws JSONException {
        if (res.has("error")) {
            deliverError(cb, res.optString("error", "שגיאה לא ידועה"));
            return;
        }
        if (!res.optBoolean("ok", false)) {
            deliverError(cb, "תגובת שרת לא תקינה");
            return;
        }
        JSONObject acc = res.getJSONObject("account");
        // The server does NOT return passwordHash - we store a placeholder.
        // The client never needs the hash after login because future calls are by username.
        UserAccount ua = new UserAccount(
                acc.getString("username"),
                acc.getString("fullName"),
                "", // server-side only
                acc.getDouble("cashBalance"));
        if (acc.has("portfolioJson") && !acc.isNull("portfolioJson")) {
            ua.setPortfolioJson(acc.getString("portfolioJson"));
        }
        deliverSuccess(cb, ua);
    }

    // =================================================================
    // HTTP plumbing
    // =================================================================

    private JSONObject httpGet(String path) throws Exception {
        return httpRequest("GET", path, null);
    }

    private JSONObject httpPost(String path, JSONObject body) throws Exception {
        return httpRequest("POST", path, body);
    }

    private JSONObject httpPut(String path, JSONObject body) throws Exception {
        return httpRequest("PUT", path, body);
    }

    private JSONObject httpRequest(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");

            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(payload);
                os.close();
            }

            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) throw new IOException("Empty response (HTTP " + code + ")");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return new JSONObject(sb.toString());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private <T> void deliverSuccess(final Callback<T> cb, final T data) {
        main.post(new Runnable() { @Override public void run() { cb.onSuccess(data); } });
    }

    private <T> void deliverError(final Callback<T> cb, final String msg) {
        Log.w(TAG, msg);
        main.post(new Runnable() { @Override public void run() { cb.onError(msg); } });
    }
}
