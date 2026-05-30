package com.example.stocktrader.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.models.Transaction;
import com.example.stocktrader.utils.Formatters;

import java.util.List;

/**
 * Adapter for the transaction history screen.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TxVH> {

    private final List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public TxVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TxVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TxVH holder, int position) {
        Transaction t = transactions.get(position);
        boolean isBuy = t.getType() == Transaction.Type.BUY;
        holder.tvType.setText(isBuy ? "קנייה" : "מכירה");
        holder.tvType.setBackgroundResource(isBuy ? R.drawable.bg_chip_buy : R.drawable.bg_chip_sell);
        holder.tvSymbol.setText(t.getSymbol());
        holder.tvDetails.setText(t.getQuantity() + " מניות @ " + Formatters.money(t.getPricePerShare()));
        holder.tvDate.setText(t.getFormattedDate());
        holder.tvTotal.setText(Formatters.money(t.getTotalValue()));

        if (!isBuy && t.getRealizedPL() != 0) {
            holder.tvPL.setVisibility(View.VISIBLE);
            holder.tvPL.setText("רווח/הפסד: " + Formatters.signedMoney(t.getRealizedPL()));
            int color = holder.itemView.getResources().getColor(
                    t.getRealizedPL() >= 0 ? R.color.positive : R.color.negative);
            holder.tvPL.setTextColor(color);
        } else {
            holder.tvPL.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return transactions == null ? 0 : transactions.size();
    }

    static class TxVH extends RecyclerView.ViewHolder {
        TextView tvType, tvSymbol, tvDetails, tvDate, tvTotal, tvPL;
        TxVH(View v) {
            super(v);
            tvType = v.findViewById(R.id.tvType);
            tvSymbol = v.findViewById(R.id.tvSymbol);
            tvDetails = v.findViewById(R.id.tvDetails);
            tvDate = v.findViewById(R.id.tvDate);
            tvTotal = v.findViewById(R.id.tvTotal);
            tvPL = v.findViewById(R.id.tvPL);
        }
    }
}
