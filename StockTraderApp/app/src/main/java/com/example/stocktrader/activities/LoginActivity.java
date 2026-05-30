package com.example.stocktrader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stocktrader.R;
import com.example.stocktrader.data.AuthManager;
import com.example.stocktrader.data.DataRepository;

/**
 * Login - now uses an async server call (AuthManager.loginAsync).
 * Shows a small ProgressBar on the button while waiting.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progress = findViewById(R.id.progress);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { attemptLogin(); }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        String lastUser = AuthManager.getInstance(this).getLastUser();
        if (!TextUtils.isEmpty(lastUser)) etUsername.setText(lastUser);
    }

    private void attemptLogin() {
        final String user = etUsername.getText().toString().trim();
        final String pwd = etPassword.getText().toString();

        if (TextUtils.isEmpty(user)) {
            etUsername.setError("נדרש שם משתמש");
            return;
        }
        if (TextUtils.isEmpty(pwd)) {
            etPassword.setError("נדרשת סיסמה");
            return;
        }

        setBusy(true);
        AuthManager.getInstance(this).loginAsync(user, pwd, new AuthManager.AsyncCallback() {
            @Override
            public void onResult(AuthManager.AuthResult res) {
                setBusy(false);
                if (!res.success) {
                    Toast.makeText(LoginActivity.this, res.errorMessage, Toast.LENGTH_LONG).show();
                    return;
                }
                DataRepository.getInstance().loginWithAccount(LoginActivity.this, res.account);
                Toast.makeText(LoginActivity.this, "ברוך הבא, " + res.account.getFullName(),
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!busy);
        btnLogin.setAlpha(busy ? 0.6f : 1f);
        btnLogin.setText(busy ? "מתחבר..." : "התחבר");
    }
}
