package com.example.stocktrader.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.stocktrader.R;
import com.example.stocktrader.data.DataRepository;
import com.example.stocktrader.data.PriceSimulator;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.models.User;
import com.example.stocktrader.utils.Formatters;

/**
 * Buy flow - quantity entered, total cost computed live, confirm via dialog.
 */
public class BuyStockActivity extends AppCompatActivity implements PriceSimulator.PriceUpdateListener {

    private TextView tvSymbol, tvCompany, tvPrice, tvCash, tvTotal, tvAfter;
    private EditText etQty;
    private Button btnQ10, btnQ50, btnQMax, btnExecute;
    private ImageButton btnBack;

    private Stock stock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_stock);

        String symbol = getIntent().getStringExtra("symbol");
        stock = DataRepository.getInstance().getStock(symbol);
        if (stock == null) { finish(); return; }

        tvSymbol = findViewById(R.id.tvSymbol);
        tvCompany = findViewById(R.id.tvCompany);
        tvPrice = findViewById(R.id.tvPrice);
        tvCash = findViewById(R.id.tvCash);
        tvTotal = findViewById(R.id.tvTotal);
        tvAfter = findViewById(R.id.tvAfter);
        etQty = findViewById(R.id.etQty);
        btnQ10 = findViewById(R.id.btnQ10);
        btnQ50 = findViewById(R.id.btnQ50);
        btnQMax = findViewById(R.id.btnQMax);
        btnExecute = findViewById(R.id.btnExecute);
        btnBack = findViewById(R.id.btnBack);

        tvSymbol.setText(stock.getSymbol());
        tvCompany.setText(stock.getCompanyName());

        etQty.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { renderCost(); }
        });

        btnQ10.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { etQty.setText("10"); }
        });
        btnQ50.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { etQty.setText("50"); }
        });
        btnQMax.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                User u = DataRepository.getInstance().getCurrentUser();
                int maxQty = (int) Math.floor(u.getCashBalance() / stock.getCurrentPrice());
                etQty.setText(String.valueOf(maxQty));
            }
        });
        btnExecute.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmBuy(); }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        PriceSimulator.getInstance().setListener(this);
        PriceSimulator.getInstance().start();
        renderCost();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PriceSimulator.getInstance().stop();
    }

    @Override
    public void onPricesUpdated() { renderCost(); }

    private int getQty() {
        String t = etQty.getText().toString().trim();
        if (TextUtils.isEmpty(t)) return 0;
        try { return Integer.parseInt(t); } catch (NumberFormatException e) { return 0; }
    }

    private void renderCost() {
        int qty = getQty();
        double price = stock.getCurrentPrice();
        double total = qty * price;
        User u = DataRepository.getInstance().getCurrentUser();
        double cash = u.getCashBalance();
        tvPrice.setText("מחיר נוכחי: " + Formatters.money(price));
        tvCash.setText("מזומן זמין: " + Formatters.money(cash));
        tvTotal.setText("עלות: " + Formatters.money(total));
        double after = cash - total;
        tvAfter.setText("יתרה לאחר ביצוע: " + Formatters.money(after));
        tvAfter.setTextColor(getResources().getColor(after >= 0 ? R.color.positive : R.color.negative));
        btnExecute.setEnabled(qty > 0 && after >= 0);
        btnExecute.setAlpha(btnExecute.isEnabled() ? 1f : 0.5f);
    }

    private void confirmBuy() {
        final int qty = getQty();
        if (qty <= 0) {
            Toast.makeText(this, "הזן כמות", Toast.LENGTH_SHORT).show();
            return;
        }
        final double price = stock.getCurrentPrice();
        new AlertDialog.Builder(this)
                .setTitle("אישור קנייה")
                .setMessage("האם לאשר קניית " + qty + " מניות " + stock.getSymbol() + " ב-" +
                        Formatters.money(price) + "?\n\nעלות כוללת: " + Formatters.money(qty * price))
                .setPositiveButton("אישור", (dialog, which) -> doBuy(qty))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void doBuy(int qty) {
        User u = DataRepository.getInstance().getCurrentUser();
        String err = DataRepository.getInstance().getPortfolio().executeBuy(u, stock, qty);
        if (err == null) {
            DataRepository.getInstance().persistState();
            Toast.makeText(this, "הקנייה בוצעה בהצלחה", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }
    }
}
