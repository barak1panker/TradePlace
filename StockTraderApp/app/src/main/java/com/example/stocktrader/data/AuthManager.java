package com.example.stocktrader.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.stocktrader.models.UserAccount;
import com.example.stocktrader.utils.PasswordUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Now backed by the remote server (ApiAuthClient).
 *
 * Local SharedPreferences are still used for two things:
 *   1. "last_user" - prefill the login screen
 *   2. Migration cache - if the user had local accounts before the server existed,
 *      we hold them here and migrate them to the server on first contact.
 *
 * All authentication and account operations are network calls.
 */
public class AuthManager {

    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "stocktrader_auth";
    private static final String KEY_ACCOUNTS = "accounts";        // legacy local accounts
    private static final String KEY_LAST_USER = "last_user";
    private static final String KEY_MIGRATED = "migrated_to_server";

    private static AuthManager instance;

    private final SharedPreferences prefs;
    /** Legacy local accounts pending migration. */
    private final Map<String, UserAccount> localAccounts;

    private AuthManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        localAccounts = new HashMap<>();
        loadLegacy();
    }

    public static synchronized AuthManager getInstance(Context ctx) {
        if (instance == null) instance = new AuthManager(ctx);
        return instance;
    }

    // ===== Public API =====

    /** Result type for register/login. */
    public static class AuthResult {
        public final boolean success;
        public final String errorMessage;
        public final UserAccount account;

        private AuthResult(boolean s, String e, UserAccount a) {
            this.success = s; this.errorMessage = e; this.account = a;
        }

        public static AuthResult ok(UserAccount account) { return new AuthResult(true, null, account); }
        public static AuthResult fail(String error) { return new AuthResult(false, error, null); }
    }

    /** Async callback for the new server-backed flows. */
    public interface AsyncCallback {
        void onResult(AuthResult result);
    }

    /**
     * Register a new account via the server.
     * Local validation runs first to give immediate UX feedback.
     */
    public void registerAsync(String username, String fullName,
                              String password, String confirmPassword,
                              final AsyncCallback cb) {
        if (username != null) username = username.trim();
        if (!PasswordUtils.isValidUsername(username)) {
            cb.onResult(AuthResult.fail("שם המשתמש חייב להכיל 3-20 תווים באנגלית/ספרות/קו תחתון"));
            return;
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            cb.onResult(AuthResult.fail("נדרש שם מלא"));
            return;
        }
        if (!PasswordUtils.isValidPassword(password)) {
            cb.onResult(AuthResult.fail("הסיסמה חייבת להכיל לפחות 6 תווים"));
            return;
        }
        if (!password.equals(confirmPassword)) {
            cb.onResult(AuthResult.fail("הסיסמאות אינן תואמות"));
            return;
        }

        ApiAuthClient.getInstance().register(username, fullName.trim(), password,
                new ApiAuthClient.Callback<UserAccount>() {
                    @Override public void onSuccess(UserAccount data) { cb.onResult(AuthResult.ok(data)); }
                    @Override public void onError(String message) { cb.onResult(AuthResult.fail(message)); }
                });
    }

    /**
     * Login via the server.
     * If we have local accounts that were never migrated, we try migrating them first.
     */
    public void loginAsync(final String username, final String password, final AsyncCallback cb) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            cb.onResult(AuthResult.fail("נדרש שם משתמש וסיסמה"));
            return;
        }

        // Migration: if the user is in the legacy local store but not yet on the server,
        // push their hash to the server first, then proceed with normal login.
        final String key = username.trim().toLowerCase();
        if (!prefs.getBoolean(KEY_MIGRATED, false) && localAccounts.containsKey(key)) {
            final UserAccount local = localAccounts.get(key);
            // Verify the password locally before migrating - otherwise we'd push
            // someone else's account if they happen to know the username.
            String candidate = PasswordUtils.hash(password, key);
            if (!candidate.equals(local.getPasswordHash())) {
                cb.onResult(AuthResult.fail("שם משתמש או סיסמה שגויים"));
                return;
            }
            Log.i(TAG, "Migrating local account to server: " + username);
            ApiAuthClient.getInstance().registerWithHash(
                    local.getUsername(),
                    local.getFullName(),
                    local.getPasswordHash(),
                    new ApiAuthClient.Callback<UserAccount>() {
                        @Override
                        public void onSuccess(UserAccount data) {
                            // After migration succeeded, do a normal login to get a fresh account object.
                            markMigrated(username.trim());
                            doServerLogin(username, password, cb);
                        }

                        @Override
                        public void onError(String message) {
                            // Account might already exist on the server (e.g. user migrated on another device).
                            // Fall through to a normal server login.
                            Log.w(TAG, "Migration failed (will try login): " + message);
                            doServerLogin(username, password, cb);
                        }
                    });
            return;
        }

        doServerLogin(username, password, cb);
    }

    private void doServerLogin(String username, String password, final AsyncCallback cb) {
        final String displayName = username;
        ApiAuthClient.getInstance().login(username, password,
                new ApiAuthClient.Callback<UserAccount>() {
                    @Override
                    public void onSuccess(UserAccount data) {
                        setLastUser(displayName);
                        cb.onResult(AuthResult.ok(data));
                    }
                    @Override
                    public void onError(String message) { cb.onResult(AuthResult.fail(message)); }
                });
    }

    /** Persist updated account fields (cash, portfolio) to the server. */
    public void saveAccountAsync(UserAccount acc, final ApiAuthClient.Callback<UserAccount> cb) {
        if (acc == null) {
            if (cb != null) cb.onError("חשבון לא תקין");
            return;
        }
        ApiAuthClient.getInstance().saveAccount(acc.getUsername(), acc.getCashBalance(),
                acc.getPortfolioJson(),
                new ApiAuthClient.Callback<UserAccount>() {
                    @Override
                    public void onSuccess(UserAccount data) {
                        if (cb != null) cb.onSuccess(data);
                    }
                    @Override
                    public void onError(String message) {
                        Log.w(TAG, "saveAccount failed: " + message);
                        if (cb != null) cb.onError(message);
                    }
                });
    }

    public void setLastUser(String username) {
        prefs.edit().putString(KEY_LAST_USER, username).apply();
    }

    public String getLastUser() {
        return prefs.getString(KEY_LAST_USER, null);
    }

    public void clearLastUser() {
        prefs.edit().remove(KEY_LAST_USER).apply();
    }

    // ===== Migration: legacy local SharedPreferences =====

    private void loadLegacy() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return;
        String stored = prefs.getString(KEY_ACCOUNTS, null);
        if (stored == null) return;
        try {
            JSONArray arr = new JSONArray(stored);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                UserAccount acc = new UserAccount(
                        o.getString("username"),
                        o.getString("fullName"),
                        o.getString("passwordHash"),
                        o.getDouble("cashBalance"));
                if (o.has("portfolioJson") && !o.isNull("portfolioJson")) {
                    acc.setPortfolioJson(o.getString("portfolioJson"));
                }
                localAccounts.put(acc.getUsername().toLowerCase(), acc);
            }
            Log.i(TAG, "Loaded " + localAccounts.size() + " legacy accounts pending migration");
        } catch (JSONException e) {
            Log.e(TAG, "Could not parse legacy accounts", e);
        }
    }

    /**
     * Mark migration complete. Only flips after at least one account migrated successfully.
     * Legacy data remains in SharedPreferences as a safety net but is no longer read.
     */
    private void markMigrated(String username) {
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
        Log.i(TAG, "Migration flag set after successful migration of " + username);
    }

    /** Legacy accounts that haven't been migrated yet, for debugging. */
    public List<UserAccount> getLegacyAccounts() {
        return new ArrayList<>(localAccounts.values());
    }
}
