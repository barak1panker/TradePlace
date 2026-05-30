package com.example.stocktrader.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.utils.Formatters;

import java.util.List;

/**
 * Adapter for the stock list (main screen, watchlist, search).
 */
public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockVH> {

    public interface OnStockClickListener {
        void onStockClick(Stock stock);
    }

    private List<Stock> stocks;
    private final OnStockClickListener listener;

    public StockAdapter(List<Stock> stocks, OnStockClickListener listener) {
        this.stocks = stocks;
        this.listener = listener;
    }

    public void updateData(List<Stock> newData) {
        this.stocks = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StockVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
        return new StockVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StockVH holder, int position) {
        final Stock s = stocks.get(position);
        holder.tvSymbol.setText(s.getSymbol());
        holder.tvCompany.setText(s.getCompanyName());
        holder.tvPrice.setText(Formatters.money(s.getCurrentPrice()));
        holder.tvChange.setText(Formatters.percent(s.getChangePercent()));
        int color = holder.itemView.getResources().getColor(
                s.isUp() ? R.color.positive : R.color.negative);
        holder.tvChange.setTextColor(color);
        holder.tvPrice.setTextColor(color);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (listener != null) listener.onStockClick(s);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stocks == null ? 0 : stocks.size();
    }

    static class StockVH extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvCompany, tvPrice, tvChange;
        StockVH(View v) {
            super(v);
            tvSymbol = v.findViewById(R.id.tvSymbol);
            tvCompany = v.findViewById(R.id.tvCompany);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvChange = v.findViewById(R.id.tvChange);
        }
    }
}
