package com.example.stocktrader.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.data.DataRepository;
import com.example.stocktrader.models.Holding;
import com.example.stocktrader.models.Stock;
import com.example.stocktrader.utils.Formatters;

import java.util.List;

/**
 * Adapter for the portfolio screen - shows each open position.
 */
public class HoldingAdapter extends RecyclerView.Adapter<HoldingAdapter.HoldingVH> {

    public interface OnHoldingClickListener {
        void onHoldingClick(Holding holding);
    }

    private List<Holding> holdings;
    private final OnHoldingClickListener listener;

    public HoldingAdapter(List<Holding> holdings, OnHoldingClickListener listener) {
        this.holdings = holdings;
        this.listener = listener;
    }

    public void updateData(List<Holding> newData) {
        this.holdings = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HoldingVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_holding, parent, false);
        return new HoldingVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HoldingVH holder, int position) {
        final Holding h = holdings.get(position);
        Stock s = DataRepository.getInstance().getStock(h.getSymbol());
        double currentPrice = s != null ? s.getCurrentPrice() : 0;
        double pl = h.unrealizedPL(currentPrice);
        double plPct = h.unrealizedPLPercent(currentPrice);

        holder.tvSymbol.setText(h.getSymbol());
        holder.tvQty.setText(h.getQuantity() + " מניות @ " + Formatters.money(h.getAverageCost()));
        holder.tvMv.setText(Formatters.money(h.marketValue(currentPrice)));
        holder.tvPL.setText(Formatters.signedMoney(pl) + " (" + Formatters.percent(plPct) + ")");
        int color = holder.itemView.getResources().getColor(
                pl >= 0 ? R.color.positive : R.color.negative);
        holder.tvPL.setTextColor(color);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (listener != null) listener.onHoldingClick(h);
            }
        });
    }

    @Override
    public int getItemCount() {
        return holdings == null ? 0 : holdings.size();
    }

    static class HoldingVH extends RecyclerView.ViewHolder {
        TextView tvSymbol, tvQty, tvMv, tvPL;
        HoldingVH(View v) {
            super(v);
            tvSymbol = v.findViewById(R.id.tvSymbol);
            tvQty = v.findViewById(R.id.tvQty);
            tvMv = v.findViewById(R.id.tvMv);
            tvPL = v.findViewById(R.id.tvPL);
        }
    }
}
