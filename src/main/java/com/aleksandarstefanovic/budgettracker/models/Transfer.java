package com.aleksandarstefanovic.budgettracker.models;

import java.sql.Date;

public class Transfer {
    // Fields
    private int id;
    private String type;
    private double amount;
    private Date date;
    private String description;
    private int categoryId;

    // Constructors
    public Transfer() {}

    public Transfer(int id, String type, double amount, Date date, String description, int categoryId) {
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
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
