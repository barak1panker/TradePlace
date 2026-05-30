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
import com.example.stocktrader.models.Holding;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.models.User;
import com.example.stocktrader.utils.Formatters;

/**
 * Sell flow - quantity entered, expected proceeds and realized P&L shown live, confirm via dialog.
 */
public class SellStockActivity extends AppCompatActivity implements PriceSimulator.PriceUpdateListener {

    private TextView tvSymbol, tvCompany, tvPrice, tvHeld, tvAvgCost, tvProceeds, tvRealized;
    private EditText etQty;
    private Button btn25, btn50, btn100, btnExecute;
    private ImageButton btnBack;

    private Stock stock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_stock);

        String symbol = getIntent().getStringExtra("symbol");
        stock = DataRepository.getInstance().getStock(symbol);
        if (stock == null) { finish(); return; }

        tvSymbol = findViewById(R.id.tvSymbol);
        tvCompany = findViewById(R.id.tvCompany);
        tvPrice = findViewById(R.id.tvPrice);
        tvHeld = findViewById(R.id.tvHeld);
        tvAvgCost = findViewById(R.id.tvAvgCost);
        tvProceeds = findViewById(R.id.tvProceeds);
        tvRealized = findViewById(R.id.tvRealized);
        etQty = findViewById(R.id.etQty);
        btn25 = findViewById(R.id.btn25);
        btn50 = findViewById(R.id.btn50);
        btn100 = findViewById(R.id.btn100);
        btnExecute = findViewById(R.id.btnExecute);
        btnBack = findViewById(R.id.btnBack);

        tvSymbol.setText(stock.getSymbol());
        tvCompany.setText(stock.getCompanyName());

        etQty.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { render(); }
        });

        btn25.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPercentage(0.25); }
        });
        btn50.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPercentage(0.50); }
        });
        btn100.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setPercentage(1.00); }
        });
        btnExecute.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmSell(); }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    private void setPercentage(double pct) {
        Holding h = DataRepository.getInstance().getPortfolio().getHolding(stock.getSymbol());
        if (h == null) return;
        int qty = (int) Math.floor(h.getQuantity() * pct);
        if (qty < 1 && h.getQuantity() >= 1) qty = 1;
        etQty.setText(String.valueOf(qty));
    }

    @Override
    protected void onResume() {
        super.onResume();
        PriceSimulator.getInstance().setListener(this);
        PriceSimulator.getInstance().start();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PriceSimulator.getInstance().stop();
    }

    @Override
    public void onPricesUpdated() { render(); }

    private int getQty() {
        String t = etQty.getText().toString().trim();
        if (TextUtils.isEmpty(t)) return 0;
        try { return Integer.parseInt(t); } catch (NumberFormatException e) { return 0; }
    }

    private void render() {
        Holding h = DataRepository.getInstance().getPortfolio().getHolding(stock.getSymbol());
        int held = h == null ? 0 : h.getQuantity();
        double avg = h == null ? 0 : h.getAverageCost();
        int qty = getQty();
        double price = stock.getCurrentPrice();
        double proceeds = qty * price;
        double realized = qty * (price - avg);

        tvPrice.setText("מחיר נוכחי: " + Formatters.money(price));
        tvHeld.setText("מניות בתיק: " + held);
        tvAvgCost.setText("מחיר ממוצע: " + Formatters.money(avg));
        tvProceeds.setText("תקבול צפוי: " + Formatters.money(proceeds));
        tvRealized.setText("רווח/הפסד ממומש: " + Formatters.signedMoney(realized));
        tvRealized.setTextColor(getResources().getColor(realized >= 0 ? R.color.positive : R.color.negative));

        boolean canSell = qty > 0 && qty <= held;
        btnExecute.setEnabled(canSell);
        btnExecute.setAlpha(canSell ? 1f : 0.5f);
    }

    private void confirmSell() {
        final int qty = getQty();
        if (qty <= 0) {
            Toast.makeText(this, "הזן כמות", Toast.LENGTH_SHORT).show();
            return;
        }
        final double price = stock.getCurrentPrice();
        new AlertDialog.Builder(this)
                .setTitle("אישור מכירה")
                .setMessage("האם לאשר מכירת " + qty + " מניות " + stock.getSymbol() + " ב-" +
                        Formatters.money(price) + "?\n\nתקבול: " + Formatters.money(qty * price))
                .setPositiveButton("אישור", (d, w) -> doSell(qty))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void doSell(int qty) {
        User u = DataRepository.getInstance().getCurrentUser();
        String err = DataRepository.getInstance().getPortfolio().executeSell(u, stock, qty);
        if (err == null) {
            DataRepository.getInstance().persistState();
            Toast.makeText(this, "המכירה בוצעה בהצלחה", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }
    }
}
