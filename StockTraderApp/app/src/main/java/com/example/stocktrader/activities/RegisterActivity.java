package com.example.stocktrader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stocktrader.R;
import com.example.stocktrader.data.AuthManager;
import com.example.stocktrader.data.DataRepository;

/**
 * Register - now uses an async server call.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etFullName, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private ImageButton btnBack;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etFullName = findViewById(R.id.etFullName);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        btnBack = findViewById(R.id.btnBack);
        progress = findViewById(R.id.progress);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { attemptRegister(); }
        });
        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        setBusy(true);
        AuthManager.getInstance(this).registerAsync(username, fullName, password, confirm,
                new AuthManager.AsyncCallback() {
                    @Override
                    public void onResult(AuthManager.AuthResult res) {
                        setBusy(false);
                        if (!res.success) {
                            Toast.makeText(RegisterActivity.this, res.errorMessage,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        DataRepository.getInstance().loginWithAccount(RegisterActivity.this, res.account);
                        Toast.makeText(RegisterActivity.this,
                                "החשבון נוצר בהצלחה! ברוך הבא " + res.account.getFullName(),
                                Toast.LENGTH_LONG).show();
                        Intent i = new Intent(RegisterActivity.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!busy);
        btnRegister.setAlpha(busy ? 0.6f : 1f);
        btnRegister.setText(busy ? "יוצר חשבון..." : "צור חשבון");
    }
}
