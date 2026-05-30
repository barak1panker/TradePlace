package com.example.stocktrader.models;

import java.io.Serializable;

/**
 * Represents a registered account in the system.
 * Distinct from User (which is the runtime user object).
 * Holds the stored credentials and the persisted portfolio JSON.
 */
public class UserAccount implements Serializable {

    private String username;
    private String fullName;
    private String passwordHash; // SHA-256 hex
    private double cashBalance;
    private String portfolioJson; // serialized portfolio - may be null

    public UserAccount(String username, String fullName, String passwordHash, double cashBalance) {
        this.username = username;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.cashBalance = cashBalance;
        this.portfolioJson = null;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public double getCashBalance() { return cashBalance; }
    public String getPortfolioJson() { return portfolioJson; }

    public void setCashBalance(double cashBalance) { this.cashBalance = cashBalance; }
    public void setPortfolioJson(String portfolioJson) { this.portfolioJson = portfolioJson; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
