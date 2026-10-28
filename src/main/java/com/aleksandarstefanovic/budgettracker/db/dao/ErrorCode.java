package com.aleksandarstefanovic.budgettracker.db.dao;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Category error codes
    CATEGORY_NAME_IS_NULL(HttpStatus.BAD_REQUEST, "Category name is required."),
    CATEGORY_NAME_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "Category name has invalid length."),
    CATEGORY_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Category name already exists in the database."),
    CATEGORY_COLOR_IS_NULL(HttpStatus.BAD_REQUEST, "Category color is required."),
    CATEGORY_COLOR_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "Category color is in invalid format."),
    CATEGORY_DOES_NOT_EXIST(HttpStatus.BAD_REQUEST, "Category with the given ID doesn't exist."),
    CATEGORY_TRANSACTIONS_EXIST(HttpStatus.CONFLICT, "Category has transactions, therefore deletion was unsuccessful."),
    CATEGORY_SQL_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "SQL error."),

    OK(HttpStatus.OK, "Request finished successfully.");

    private final HttpStatus status;
    private final String description;

    ErrorCode(HttpStatus status, String description) {
        this.status = status;
        this.description = description;
    }

    public HttpStatus getStatus(){
        return status;
    }

    public String getDescription() {
        return description;
    }
}
