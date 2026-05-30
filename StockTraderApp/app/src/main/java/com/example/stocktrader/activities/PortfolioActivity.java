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
import com.example.stocktrader.adapters.HoldingAdapter;
import com.example.stocktrader.data.DataRepository;
import com.example.stocktrader.data.PriceSimulator;
import com.example.stocktrader.models.Holding;
import com.example.stocktrader.utils.Formatters;

import java.util.ArrayList;

/**
 * Lists the user's open positions and overall portfolio numbers.
 */
public class PortfolioActivity extends AppCompatActivity implements PriceSimulator.PriceUpdateListener {

    private TextView tvTotalValue, tvTotalCost, tvUnrealized, tvCash, tvEmpty;
    private RecyclerView rv;
    private HoldingAdapter adapter;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvUnrealized = findViewById(R.id.tvUnrealized);
        tvCash = findViewById(R.id.tvCash);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv = findViewById(R.id.rvHoldings);
        btnBack = findViewById(R.id.btnBack);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HoldingAdapter(new ArrayList<>(), new HoldingAdapter.OnHoldingClickListener() {
            @Override
            public void onHoldingClick(Holding h) {
                Intent i = new Intent(PortfolioActivity.this, StockDetailActivity.class);
                i.putExtra("symbol", h.getSymbol());
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
    public void onPricesUpdated() { render(); }

    private void render() {
        DataRepository repo = DataRepository.getInstance();
        double cash = repo.getCurrentUser().getCashBalance();
        double mv = repo.getPortfolio().getTotalMarketValue(repo);
        double cost = repo.getPortfolio().getTotalCostBasis();
        double pl = repo.getPortfolio().getTotalUnrealizedPL(repo);
        tvTotalValue.setText(Formatters.money(cash + mv));
        tvTotalCost.setText("עלות מצטברת: " + Formatters.money(cost));
        tvUnrealized.setText("רווח/הפסד פתוח: " + Formatters.signedMoney(pl));
        tvUnrealized.setTextColor(getResources().getColor(pl >= 0 ? R.color.positive : R.color.negative));
        tvCash.setText("מזומן: " + Formatters.money(cash));

        adapter.updateData(new ArrayList<>(repo.getPortfolio().getHoldings().values()));
        boolean empty = repo.getPortfolio().getHoldings().isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
