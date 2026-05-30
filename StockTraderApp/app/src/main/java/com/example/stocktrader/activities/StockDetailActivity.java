package com.example.stocktrader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.adapters.NewsAdapter;
import com.example.stocktrader.data.ApiClient;
import com.example.stocktrader.data.DataRepository;
import com.example.stocktrader.data.PriceSimulator;
import com.example.stocktrader.models.Holding;
import com.example.stocktrader.models.NewsArticle;
import com.example.stocktrader.models.Portfolio;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.utils.Formatters;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Stock details + live data from Alpha Vantage (historical chart) and NewsAPI (headlines).
 * The live price ticker (PriceSimulator) keeps fluctuating in the background to
 * give the impression of a live market between API fetches.
 */
public class StockDetailActivity extends AppCompatActivity implements PriceSimulator.PriceUpdateListener {

    private TextView tvSymbol, tvCompany, tvSector, tvPrice, tvChange,
            tvOpen, tvHigh, tvLow, tvVolume, tvPrevClose, tvPositionTitle, tvPositionDetails,
            tvChartStatus, tvNewsStatus;
    private Button btnBuy, btnSell;
    private ImageButton btnWatch, btnBack;
    private LineChart chart;
    private ProgressBar chartProgress, newsProgress;
    private RecyclerView rvNews;
    private NewsAdapter newsAdapter;

    private Stock stock;
    private List<String> chartLabels = new ArrayList<>();
    private List<ApiClient.PricePoint> historicalData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        String symbol = getIntent().getStringExtra("symbol");
        stock = DataRepository.getInstance().getStock(symbol);
        if (stock == null) {
            Toast.makeText(this, "מניה לא נמצאה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupChart();
        setupNewsList();
        setupActions();
        render();

        // Live data fetches
        fetchHistoricalChart();
        fetchNews();
    }

    private void bindViews() {
        tvSymbol = findViewById(R.id.tvSymbol);
        tvCompany = findViewById(R.id.tvCompany);
        tvSector = findViewById(R.id.tvSector);
        tvPrice = findViewById(R.id.tvPrice);
        tvChange = findViewById(R.id.tvChange);
        tvOpen = findViewById(R.id.tvOpen);
        tvHigh = findViewById(R.id.tvHigh);
        tvLow = findViewById(R.id.tvLow);
        tvVolume = findViewById(R.id.tvVolume);
        tvPrevClose = findViewById(R.id.tvPrevClose);
        tvPositionTitle = findViewById(R.id.tvPositionTitle);
        tvPositionDetails = findViewById(R.id.tvPositionDetails);
        tvChartStatus = findViewById(R.id.tvChartStatus);
        tvNewsStatus = findViewById(R.id.tvNewsStatus);
        btnBuy = findViewById(R.id.btnBuy);
        btnSell = findViewById(R.id.btnSell);
        btnWatch = findViewById(R.id.btnWatch);
        btnBack = findViewById(R.id.btnBack);
        chart = findViewById(R.id.chart);
        chartProgress = findViewById(R.id.chartProgress);
        newsProgress = findViewById(R.id.newsProgress);
        rvNews = findViewById(R.id.rvNews);
    }

    private void setupChart() {
        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.getLegend().setEnabled(false);
        chart.setNoDataText("");

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-30f);
        xAxis.setTextSize(9f);

        YAxis right = chart.getAxisRight();
        right.setEnabled(false);
    }

    private void setupNewsList() {
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        rvNews.setNestedScrollingEnabled(false);
        newsAdapter = new NewsAdapter(new ArrayList<NewsArticle>());
        rvNews.setAdapter(newsAdapter);
    }

    private void setupActions() {
        btnBuy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(StockDetailActivity.this, BuyStockActivity.class);
                i.putExtra("symbol", stock.getSymbol());
                startActivity(i);
            }
        });
        btnSell.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(StockDetailActivity.this, SellStockActivity.class);
                i.putExtra("symbol", stock.getSymbol());
                startActivity(i);
            }
        });
        btnWatch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Portfolio p = DataRepository.getInstance().getPortfolio();
                if (p.isInWatchlist(stock.getSymbol())) {
                    p.removeFromWatchlist(stock.getSymbol());
                    Toast.makeText(StockDetailActivity.this, "הוסר מרשימת מעקב", Toast.LENGTH_SHORT).show();
                } else {
                    p.addToWatchlist(stock.getSymbol());
                    Toast.makeText(StockDetailActivity.this, "נוסף לרשימת מעקב", Toast.LENGTH_SHORT).show();
                }
                DataRepository.getInstance().persistState();
                updateWatchIcon();
            }
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
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PriceSimulator.getInstance().stop();
    }

    @Override
    public void onPricesUpdated() {
        // Don't redraw the historical chart on each tick - just refresh the header values.
        renderHeader();
        renderPosition();
    }

    // =============================================================
    // Rendering
    // =============================================================

    private void render() {
        renderHeader();
        renderPosition();
        updateWatchIcon();
        if (historicalData != null) renderChart(historicalData);
    }

    private void renderHeader() {
        tvSymbol.setText(stock.getSymbol());
        tvCompany.setText(stock.getCompanyName());
        tvSector.setText(stock.getSector());
        tvPrice.setText(Formatters.money(stock.getCurrentPrice()));
        tvChange.setText(Formatters.signedMoney(stock.getChange())
                + "  (" + Formatters.percent(stock.getChangePercent()) + ")");
        tvChange.setTextColor(getResources().getColor(
                stock.isUp() ? R.color.positive : R.color.negative));

        tvOpen.setText("פתיחה: " + Formatters.money(stock.getOpenPrice()));
        tvHigh.setText("גבוה: " + Formatters.money(stock.getDayHigh()));
        tvLow.setText("נמוך: " + Formatters.money(stock.getDayLow()));
        tvVolume.setText("נפח: " + Formatters.whole(stock.getVolume()));
        tvPrevClose.setText("סגירה קודמת: " + Formatters.money(stock.getPreviousClose()));
    }

    private void renderChart(List<ApiClient.PricePoint> data) {
        if (data == null || data.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        chartLabels.clear();
        for (int i = 0; i < data.size(); i++) {
            ApiClient.PricePoint p = data.get(i);
            entries.add(new Entry(i, (float) p.close));
            // Show MM-DD format
            String label = p.date;
            if (label != null && label.length() >= 10) label = label.substring(5);
            chartLabels.add(label);
        }
        boolean isUp = data.get(data.size() - 1).close >= data.get(0).close;
        int color = getResources().getColor(isUp ? R.color.positive : R.color.negative);

        LineDataSet ds = new LineDataSet(entries, "Price");
        ds.setColor(color);
        ds.setLineWidth(2.2f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setDrawFilled(true);
        ds.setFillColor(color);
        ds.setFillAlpha(40);
        chart.setData(new LineData(ds));

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(chartLabels));
        chart.invalidate();
    }

    private void renderPosition() {
        Portfolio p = DataRepository.getInstance().getPortfolio();
        Holding h = p.getHolding(stock.getSymbol());
        if (h == null || h.getQuantity() == 0) {
            tvPositionTitle.setText("פוזיציה נוכחית");
            tvPositionDetails.setText("אין מניות בתיק");
            btnSell.setEnabled(false);
            btnSell.setAlpha(0.4f);
        } else {
            tvPositionTitle.setText("פוזיציה נוכחית: " + h.getQuantity() + " מניות");
            double pl = h.unrealizedPL(stock.getCurrentPrice());
            double plPct = h.unrealizedPLPercent(stock.getCurrentPrice());
            tvPositionDetails.setText("מחיר ממוצע: " + Formatters.money(h.getAverageCost())
                    + "\nשווי שוק: " + Formatters.money(h.marketValue(stock.getCurrentPrice()))
                    + "\nרווח/הפסד: " + Formatters.signedMoney(pl) + " (" + Formatters.percent(plPct) + ")");
            btnSell.setEnabled(true);
            btnSell.setAlpha(1f);
        }
    }

    private void updateWatchIcon() {
        Portfolio p = DataRepository.getInstance().getPortfolio();
        boolean inList = p.isInWatchlist(stock.getSymbol());
        btnWatch.setImageResource(inList ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    // =============================================================
    // Network
    // =============================================================

    private void fetchHistoricalChart() {
        chartProgress.setVisibility(View.VISIBLE);
        tvChartStatus.setVisibility(View.VISIBLE);
        tvChartStatus.setText("טוען נתוני שוק חיים...");
        ApiClient.getInstance().fetchStockHistory(stock.getSymbol(),
                new ApiClient.Callback<List<ApiClient.PricePoint>>() {
                    @Override
                    public void onSuccess(List<ApiClient.PricePoint> data) {
                        chartProgress.setVisibility(View.GONE);
                        if (data.isEmpty()) {
                            tvChartStatus.setText("אין נתונים זמינים");
                            return;
                        }
                        tvChartStatus.setVisibility(View.GONE);
                        historicalData = data;
                        // Update day stats from the latest API close, if available
                        ApiClient.PricePoint last = data.get(data.size() - 1);
                        ApiClient.PricePoint prev = data.size() >= 2 ? data.get(data.size() - 2) : last;
                        // Sync the simulator's view of the price to match the real one
                        stock.setCurrentPrice(last.close);
                        // Use the second-to-last as previous close (rough but reasonable)
                        renderHeader();
                        renderChart(data);
                    }

                    @Override
                    public void onError(String message) {
                        chartProgress.setVisibility(View.GONE);
                        tvChartStatus.setText("גרף זמני (לא נטענו נתונים אמיתיים): " + message);
                        // Fall back to the simulator's price history
                        renderSyntheticChart();
                    }
                });
    }

    /** Backup chart from the simulator's running price history when the API fails. */
    private void renderSyntheticChart() {
        List<Double> history = stock.getPriceHistory();
        if (history == null || history.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        chartLabels.clear();
        for (int i = 0; i < history.size(); i++) {
            entries.add(new Entry(i, history.get(i).floatValue()));
            chartLabels.add(String.valueOf(i));
        }
        int color = getResources().getColor(stock.isUp() ? R.color.positive : R.color.negative);
        LineDataSet ds = new LineDataSet(entries, "Price");
        ds.setColor(color);
        ds.setLineWidth(2.2f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setDrawFilled(true);
        ds.setFillColor(color);
        ds.setFillAlpha(40);
        chart.setData(new LineData(ds));
        chart.invalidate();
    }

    private void fetchNews() {
        newsProgress.setVisibility(View.VISIBLE);
        tvNewsStatus.setVisibility(View.VISIBLE);
        tvNewsStatus.setText("טוען חדשות...");
        // Use company name when available - generally better than ticker symbol
        String query = stock.getCompanyName();
        if (query == null || query.isEmpty()) query = stock.getSymbol();
        ApiClient.getInstance().fetchNews(query, new ApiClient.Callback<List<NewsArticle>>() {
            @Override
            public void onSuccess(List<NewsArticle> data) {
                newsProgress.setVisibility(View.GONE);
                if (data.isEmpty()) {
                    tvNewsStatus.setText("אין כתבות זמינות");
                    return;
                }
                tvNewsStatus.setVisibility(View.GONE);
                newsAdapter.updateData(data);
            }

            @Override
            public void onError(String message) {
                newsProgress.setVisibility(View.GONE);
                tvNewsStatus.setText("לא ניתן לטעון חדשות: " + message);
            }
        });
    }
}
