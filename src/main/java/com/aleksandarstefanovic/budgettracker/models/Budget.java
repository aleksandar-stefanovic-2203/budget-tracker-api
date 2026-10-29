package com.aleksandarstefanovic.budgettracker.models;

import java.sql.Date;

public class Budget {
    // Fields
    private Integer id;
    private Date month;
    private Integer category_id;
    private Double amount;

    // Constructors
    public Budget() {}

    public Budget(Integer id, Date month, Integer categoryId, Double amount) {
        this.id = id;
        this.month = month;
        this.category_id = categoryId;
        this.amount = amount;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public Date getMonth() {
        return month;
    }

    public void setMonth(Date month) {
        this.month = month;
    }

    public Integer getCategory_id() {
        return category_id;
    }

    public void setCategory_id(Integer categoryId) {
        this.category_id = categoryId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
