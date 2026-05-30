package com.example.stocktrader.models;

import java.io.Serializable;

/**
 * Represents the logged-in trader.
 */
public class User implements Serializable {

    private String username;
    private String fullName;
    private double cashBalance;

    public User(String username, String fullName, double cashBalance) {
        this.username = username;
        this.fullName = fullName;
        this.cashBalance = cashBalance;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public double getCashBalance() { return cashBalance; }

    public void deposit(double amount) {
        if (amount > 0) cashBalance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount > 0 && cashBalance >= amount) {
            cashBalance -= amount;
            return true;
        }
        return false;
    }
}
