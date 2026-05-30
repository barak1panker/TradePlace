package com.example.stocktrader.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stocktrader.R;
import com.example.stocktrader.data.DataRepository;
import com.example.stocktrader.data.PriceSimulator;
import com.example.stocktrader.models.Holding;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.models.Transaction;
import com.example.stocktrader.utils.Formatters;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Portfolio analytics: P&L summary, best/worst performer, sector breakdown pie.
 */
public class AnalyticsActivity extends AppCompatActivity implements PriceSimulator.PriceUpdateListener {

    private TextView tvTotalReturn, tvRealized, tvUnrealized, tvTrades, tvBest, tvWorst, tvWinRate;
    private PieChart pieChart;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        tvTotalReturn = findViewById(R.id.tvTotalReturn);
        tvRealized = findViewById(R.id.tvRealized);
        tvUnrealized = findViewById(R.id.tvUnrealized);
        tvTrades = findViewById(R.id.tvTrades);
        tvBest = findViewById(R.id.tvBest);
        tvWorst = findViewById(R.id.tvWorst);
        tvWinRate = findViewById(R.id.tvWinRate);
        pieChart = findViewById(R.id.pieChart);
        btnBack = findViewById(R.id.btnBack);

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
        double realized = repo.getPortfolio().getTotalRealizedPL();
        double unrealized = repo.getPortfolio().getTotalUnrealizedPL(repo);
        double total = realized + unrealized;

        tvTotalReturn.setText(Formatters.signedMoney(total));
        tvTotalReturn.setTextColor(getResources().getColor(total >= 0 ? R.color.positive : R.color.negative));
        tvRealized.setText("רווח/הפסד ממומש: " + Formatters.signedMoney(realized));
        tvUnrealized.setText("רווח/הפסד פתוח: " + Formatters.signedMoney(unrealized));

        List<Transaction> txs = repo.getPortfolio().getTransactions();
        tvTrades.setText("סה\"כ פעולות: " + txs.size());

        // Best / worst by unrealized P&L percent
        Holding best = null, worst = null;
        double bestPct = -Double.MAX_VALUE, worstPct = Double.MAX_VALUE;
        for (Holding h : repo.getPortfolio().getHoldings().values()) {
            Stock s = repo.getStock(h.getSymbol());
            if (s == null) continue;
            double pct = h.unrealizedPLPercent(s.getCurrentPrice());
            if (pct > bestPct) { bestPct = pct; best = h; }
            if (pct < worstPct) { worstPct = pct; worst = h; }
        }
        tvBest.setText(best == null ? "ביצועי שיא: -" :
                "ביצועי שיא: " + best.getSymbol() + "  " + Formatters.percent(bestPct));
        tvWorst.setText(worst == null ? "ביצועי שפל: -" :
                "ביצועי שפל: " + worst.getSymbol() + "  " + Formatters.percent(worstPct));

        // Win rate from sells
        int sells = 0, wins = 0;
        for (Transaction t : txs) {
            if (t.getType() == Transaction.Type.SELL) {
                sells++;
                if (t.getRealizedPL() > 0) wins++;
            }
        }
        String winRate = sells == 0 ? "-" : Math.round((wins * 100.0) / sells) + "%";
        tvWinRate.setText("אחוז עסקאות מרוויחות: " + winRate + "  (" + wins + "/" + sells + ")");

        renderPie();
    }

    private void renderPie() {
        DataRepository repo = DataRepository.getInstance();
        List<PieEntry> entries = new ArrayList<>();
        for (Holding h : repo.getPortfolio().getHoldings().values()) {
            Stock s = repo.getStock(h.getSymbol());
            if (s == null) continue;
            float val = (float) h.marketValue(s.getCurrentPrice());
            if (val > 0) entries.add(new PieEntry(val, h.getSymbol()));
        }
        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("אין נתונים להצגה");
            pieChart.invalidate();
            return;
        }
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(ColorTemplate.MATERIAL_COLORS);
        ds.setValueTextSize(11f);
        ds.setSliceSpace(2f);
        PieData data = new PieData(ds);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.invalidate();
    }
}
