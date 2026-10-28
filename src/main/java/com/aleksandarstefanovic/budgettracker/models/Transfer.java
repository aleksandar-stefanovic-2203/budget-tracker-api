package com.aleksandarstefanovic.budgettracker.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transfer {
    // Fields
    private int id;
    private String type;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private int categoryId;

    // Constructors
    public Transfer() {}

    public Transfer(int id, String type, BigDecimal amount, LocalDate date, String description, int categoryId) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.categoryId = categoryId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}
