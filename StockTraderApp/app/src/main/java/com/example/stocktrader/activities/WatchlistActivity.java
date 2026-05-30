package com.example.stocktrader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.adapters.StockAdapter;
import com.example.stocktrader.data.DataRepository;
import com.example.stocktrader.data.PriceSimulator;
import com.example.stocktrader.models.Stock;

import java.util.ArrayList;
import java.util.List;

/**
 * Stocks the user has added to their watchlist. Same row UI as the main list.
 */
public class WatchlistActivity extends AppCompatActivity implements PriceSimulator.PriceUpdateListener {

    private RecyclerView rv;
    private StockAdapter adapter;
    private TextView tvEmpty;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchlist);

        rv = findViewById(R.id.rvWatchlist);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StockAdapter(new ArrayList<>(), new StockAdapter.OnStockClickListener() {
            @Override public void onStockClick(Stock stock) {
                Intent i = new Intent(WatchlistActivity.this, StockDetailActivity.class);
                i.putExtra("symbol", stock.getSymbol());
                startActivity(i);
            }
        });
        rv.setAdapter(adapter);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
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
    public void onPricesUpdated() {
        adapter.notifyDataSetChanged();
    }

    private void render() {
        List<String> symbols = DataRepository.getInstance().getPortfolio().getWatchlist();
        List<Stock> stocks = new ArrayList<>();
        for (String sym : symbols) {
            Stock s = DataRepository.getInstance().getStock(sym);
            if (s != null) stocks.add(s);
        }
        adapter.updateData(stocks);
        boolean empty = stocks.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
