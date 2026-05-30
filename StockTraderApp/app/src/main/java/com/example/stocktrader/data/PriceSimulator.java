package com.example.stocktrader.data;

import android.os.Handler;
import android.os.Looper;

import com.example.stocktrader.models.Stock;

import java.util.List;
import java.util.Random;

/**
 * Simulates a live market - every few seconds the prices fluctuate.
 * Listeners are notified on the main thread.
 */
public class PriceSimulator {

    public interface PriceUpdateListener {
        void onPricesUpdated();
    }

    private static PriceSimulator instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private PriceUpdateListener listener;
    private boolean running = false;
    private static final long TICK_MS = 3000; // 3-second tick

    private PriceSimulator() {}

    public static synchronized PriceSimulator getInstance() {
        if (instance == null) instance = new PriceSimulator();
        return instance;
    }

    public void setListener(PriceUpdateListener l) {
        this.listener = l;
    }

    public void start() {
        if (running) return;
        running = true;
        handler.postDelayed(tick, TICK_MS);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updatePrices();
            if (listener != null) listener.onPricesUpdated();
            if (running) handler.postDelayed(this, TICK_MS);
        }
    };

    /** Apply a small random walk to each stock. */
    private void updatePrices() {
        List<Stock> stocks = DataRepository.getInstance().getAllStocks();
        for (Stock s : stocks) {
            // pct change between -1.5% and +1.5% with a tiny upward drift
            double pct = (random.nextDouble() - 0.49) * 0.03;
            double newPrice = s.getCurrentPrice() * (1 + pct);
            if (newPrice < 0.5) newPrice = 0.5;
            s.setCurrentPrice(roundTo2(newPrice));
        }
    }

    private double roundTo2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
