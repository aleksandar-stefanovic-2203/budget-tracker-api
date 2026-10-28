package com.aleksandarstefanovic.budgettracker.models;

import java.sql.Date;

public class Budget {
    // Fields
    private Date month;
    private int categoryId;
    private double amount;

    // Constructors
    public Budget() {}

    public Budget(Date month, int categoryId, double amount) {
        this.month = month;
        this.categoryId = categoryId;
        this.amount = amount;
    }

    // Getters and Setters
    public Date getMonth() {
        return month;
    }

    public void setMonth(Date month) {
        this.month = month;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
