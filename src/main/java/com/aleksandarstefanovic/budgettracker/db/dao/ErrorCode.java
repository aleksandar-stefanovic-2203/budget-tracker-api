package com.aleksandarstefanovic.budgettracker.db.dao;

public enum ErrorCode {
    CATEGORY_NAME_IS_NULL,
    CATEGORY_NAME_NOT_STRIPPED,
    CATEGORY_NAME_INVALID_LENGTH, // TODO: define bounds on a single place
    CATEGORY_COLOR_IS_NULL,
    CATEGORY_COLOR_INVALID_FORMAT,
    CATEGORY_SQL_ERROR,

    NO_ERRORS
}
