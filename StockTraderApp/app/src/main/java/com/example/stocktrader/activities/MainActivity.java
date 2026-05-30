package com.example.stocktrader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.adapters.StockAdapter;
import com.example.stocktrader.data.DataRepository;
import com.example.stocktrader.data.PriceSimulator;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.models.User;
import com.example.stocktrader.utils.Formatters;

import java.util.List;

/**
 * Main dashboard.
 * Shows portfolio summary, quick nav, top movers and a searchable stock list.
 */
public class MainActivity extends AppCompatActivity implements PriceSimulator.PriceUpdateListener {

    private TextView tvGreeting, tvPortfolioValue, tvCash, tvDayPL;
    private RecyclerView rvStocks;
    private EditText etSearch;
    private StockAdapter stockAdapter;

    private LinearLayout cardPortfolio, cardWatchlist, cardHistory, cardAnalytics;
    private ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // If the process was killed and there is no user, send back to login.
        if (DataRepository.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        bindViews();
        setupRecycler();
        setupSearch();
        setupQuickNav();
    }

    private void bindViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvPortfolioValue = findViewById(R.id.tvPortfolioValue);
        tvCash = findViewById(R.id.tvCash);
        tvDayPL = findViewById(R.id.tvDayPL);
        rvStocks = findViewById(R.id.rvStocks);
        etSearch = findViewById(R.id.etSearch);
        cardPortfolio = findViewById(R.id.cardPortfolio);
        cardWatchlist = findViewById(R.id.cardWatchlist);
        cardHistory = findViewById(R.id.cardHistory);
        cardAnalytics = findViewById(R.id.cardAnalytics);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupRecycler() {
        rvStocks.setLayoutManager(new LinearLayoutManager(this));
        stockAdapter = new StockAdapter(DataRepository.getInstance().getAllStocks(),
                new StockAdapter.OnStockClickListener() {
                    @Override
                    public void onStockClick(Stock stock) {
                        Intent i = new Intent(MainActivity.this, StockDetailActivity.class);
                        i.putExtra("symbol", stock.getSymbol());
                        startActivity(i);
                    }
                });
        rvStocks.setAdapter(stockAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                List<Stock> results = DataRepository.getInstance().searchStocks(s.toString());
                stockAdapter.updateData(results);
            }
        });
    }

    private void setupQuickNav() {
        cardPortfolio.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PortfolioActivity.class));
            }
        });
        cardWatchlist.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WatchlistActivity.class));
            }
        });
        cardHistory.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TransactionsActivity.class));
            }
        });
        cardAnalytics.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AnalyticsActivity.class));
            }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                DataRepository.getInstance().logout();
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        PriceSimulator.getInstance().setListener(this);
        PriceSimulator.getInstance().start();
        refreshSummary();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PriceSimulator.getInstance().stop();
        // Save user balance on pause (prices may have changed mid-session is fine; cash matters)
        DataRepository.getInstance().persistState();
    }

    @Override
    public void onPricesUpdated() {
        refreshSummary();
        stockAdapter.notifyDataSetChanged();
    }

    private void refreshSummary() {
        DataRepository repo = DataRepository.getInstance();
        User user = repo.getCurrentUser();
        if (user == null) return;
        tvGreeting.setText("שלום, " + user.getFullName());
        double cash = user.getCashBalance();
        double mv = repo.getPortfolio().getTotalMarketValue(repo);
        double pl = repo.getPortfolio().getTotalUnrealizedPL(repo);
        tvPortfolioValue.setText(Formatters.money(cash + mv));
        tvCash.setText("מזומן: " + Formatters.money(cash));
        tvDayPL.setText("רווח/הפסד שוטף: " + Formatters.signedMoney(pl));
        tvDayPL.setTextColor(getResources().getColor(
                pl >= 0 ? R.color.positive : R.color.negative));
    }
}
