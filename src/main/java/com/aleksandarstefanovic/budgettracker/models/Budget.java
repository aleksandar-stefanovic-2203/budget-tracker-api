package com.aleksandarstefanovic.budgettracker.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Budget {
    // Fields
    private LocalDate month;
    private int categoryId;
    private BigDecimal amount;

    // Constructors
    public Budget() {}

    public Budget(LocalDate month, int categoryId, BigDecimal amount) {
        this.month = month;
        this.categoryId = categoryId;
        this.amount = amount;
    }

    // Getters and Setters
    public LocalDate getMonth() {
        return month;
    }

    public void setMonth(LocalDate month) {
        this.month = month;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
