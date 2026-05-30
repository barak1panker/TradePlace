package com.example.stocktrader.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stocktrader.R;
import com.example.stocktrader.adapters.TransactionAdapter;
import com.example.stocktrader.data.DataRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.stocktrader.models.Transaction;

/**
 * Lists every buy/sell action the user has made. Newest first.
 */
public class TransactionsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvEmpty;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        rv = findViewById(R.id.rvTransactions);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);

        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Transaction> txs = new ArrayList<>(DataRepository.getInstance().getPortfolio().getTransactions());
        Collections.reverse(txs);
        rv.setAdapter(new TransactionAdapter(txs));
        if (txs.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        }
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }
}
