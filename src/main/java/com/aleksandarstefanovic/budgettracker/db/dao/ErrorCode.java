package com.aleksandarstefanovic.budgettracker.db.dao;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Category error codes
    CATEGORY_ID_IS_NULL(HttpStatus.BAD_REQUEST, "Category ID is required."),
    CATEGORY_ID_OUT_OF_BOUNDS(HttpStatus.BAD_REQUEST, "Category ID is out of bounds."),
    CATEGORY_NAME_IS_NULL(HttpStatus.BAD_REQUEST, "Category name is required."),
    CATEGORY_NAME_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "Category name has invalid length."),
    CATEGORY_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Category name already exists in the database."),
    CATEGORY_COLOR_IS_NULL(HttpStatus.BAD_REQUEST, "Category color is required."),
    CATEGORY_COLOR_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "Category color is in invalid format."),
    CATEGORY_DOES_NOT_EXIST(HttpStatus.BAD_REQUEST, "Category with the given ID doesn't exist."),
    CATEGORY_TRANSACTIONS_EXIST(HttpStatus.CONFLICT, "Category has transactions, therefore deletion was unsuccessful."),

    // Budget error codes
    BUDGET_ID_IS_NULL(HttpStatus.BAD_REQUEST, "Budget ID is required."),
    BUDGET_ID_OUT_OF_BOUNDS(HttpStatus.BAD_REQUEST, "Budget ID is out of bounds."),
    BUDGET_MONTH_IS_NULL(HttpStatus.BAD_REQUEST, "Budget month is required."),
    BUDGET_MONTH_INCORRECT_FORMAT(HttpStatus.BAD_REQUEST, "Budget month is in incorrect format. Correct format is YYYY-MM-01 (first day of the month)."),
    BUDGET_CATEGORY_IS_NULL(HttpStatus.BAD_REQUEST, "Budget category is required."),
    BUDGET_CATEGORY_DOES_NOT_EXIST(HttpStatus.UNPROCESSABLE_CONTENT, "Budget category doesn't exist in the database."),
    BUDGET_AMOUNT_IS_NULL(HttpStatus.BAD_REQUEST, "Budget amount is required."),
    BUDGET_AMOUNT_OUT_OF_BOUNDS(HttpStatus.BAD_REQUEST, "Budget amount is out of bounds."),
    BUDGET_AMOUNT_INVALID_DECIMAL_PLACES(HttpStatus.BAD_REQUEST, "Budget amount contains too many decimal places."),
    BUDGET_EXISTS(HttpStatus.CONFLICT, "Budget already exists for the given month and category."),
    BUDGET_DOES_NOT_EXIST(HttpStatus.BAD_REQUEST, "Budget with the given ID doesn't exist."),

    // Transaction error codes
    TRANSACTION_ID_IS_NULL(HttpStatus.BAD_REQUEST, "Transaction ID is required."),
    TRANSACTION_ID_OUT_OF_BOUNDS(HttpStatus.BAD_REQUEST, "Transaction ID is out of bounds."),
    TRANSACTION_TYPE_IS_NULL(HttpStatus.BAD_REQUEST, "Transaction type is required."),
    TRANSACTION_TYPE_INVALID(HttpStatus.BAD_REQUEST, "Transaction type must be INCOME or EXPENSE."),
    TRANSACTION_AMOUNT_IS_NULL(HttpStatus.BAD_REQUEST, "Transaction amount is required."),
    TRANSACTION_AMOUNT_OUT_OF_BOUNDS(HttpStatus.BAD_REQUEST, "Transaction amount is out of bounds."),
    TRANSACTION_AMOUNT_INVALID_DECIMAL_PLACES(HttpStatus.BAD_REQUEST, "Transaction amount contains too many decimal places."),
    TRANSACTION_DATE_IS_NULL(HttpStatus.BAD_REQUEST, "Transaction date is required."),
    TRANSACTION_DESCRIPTION_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "Transaction description cannot exceed 200 characters."),
    TRANSACTION_CATEGORY_IS_NULL(HttpStatus.BAD_REQUEST, "Transaction category is required."),
    TRANSACTION_CATEGORY_DOES_NOT_EXIST(HttpStatus.UNPROCESSABLE_CONTENT, "Transaction category doesn't exist in the database."),
    TRANSACTION_DOES_NOT_EXIST(HttpStatus.NOT_FOUND, "Transaction with the given ID doesn't exist."),
    TRANSACTION_PAGE_INVALID(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 1."),
    TRANSACTION_PAGE_SIZE_INVALID(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100."),
    TRANSACTION_DATE_RANGE_INVALID(HttpStatus.BAD_REQUEST, "Start date must not be after end date."),
    TRANSACTION_CATEGORY_NAME_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "Category name filter cannot exceed 30 characters."),

    OK(HttpStatus.OK, "Request finished successfully."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service is not avaliable at the moment.");

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
