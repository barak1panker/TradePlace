package com.example.stocktrader.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.models.NewsArticle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Adapter for the news list inside the stock detail screen.
 * Each item opens the article URL in the default browser.
 */
public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsVH> {

    private List<NewsArticle> articles;

    public NewsAdapter(List<NewsArticle> articles) {
        this.articles = articles;
    }

    public void updateData(List<NewsArticle> data) {
        this.articles = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsVH holder, int position) {
        final NewsArticle a = articles.get(position);
        holder.tvTitle.setText(TextUtils.isEmpty(a.getTitle()) ? "-" : a.getTitle());
        holder.tvDesc.setText(TextUtils.isEmpty(a.getDescription()) ? "" : a.getDescription());
        holder.tvDesc.setVisibility(TextUtils.isEmpty(a.getDescription()) ? View.GONE : View.VISIBLE);
        holder.tvSource.setText(a.getSourceName());
        holder.tvDate.setText(formatDate(a.getPublishedAt()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context ctx = v.getContext();
                if (TextUtils.isEmpty(a.getUrl())) {
                    Toast.makeText(ctx, "אין קישור זמין", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(a.getUrl()));
                    ctx.startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(ctx, "לא ניתן לפתוח את הקישור", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() { return articles == null ? 0 : articles.size(); }

    /** "2025-04-12T15:30:00Z" -> "12/04/2025 18:30" (local time best-effort). */
    private String formatDate(String iso) {
        if (TextUtils.isEmpty(iso)) return "";
        try {
            SimpleDateFormat src = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            src.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = src.parse(iso);
            if (d == null) return iso;
            SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return out.format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    static class NewsVH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvSource, tvDate;
        NewsVH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvDesc = v.findViewById(R.id.tvDesc);
            tvSource = v.findViewById(R.id.tvSource);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }
}
