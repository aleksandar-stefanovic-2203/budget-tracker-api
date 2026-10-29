package com.aleksandarstefanovic.budgettracker.db.dao.validation;

import java.math.BigDecimal;
import java.sql.Date;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;

public class BudgetValidator {
    private static final Double MIN_AMOUNT_VALUE = 0.00;
    private static final Double MAX_AMOUNT_VALUE = 1000000.00;
    private static final Integer MAX_NUMBER_OF_DECIMAL_PLACES = 2;

    public static ErrorCode isBudgetIDValid(Integer id){
        if(id == null) return ErrorCode.BUDGET_ID_IS_NULL;
        if(id <= 0) return ErrorCode.BUDGET_ID_OUT_OF_BOUNDS;

        return ErrorCode.OK;
    }

    public static ErrorCode isBudgetMonthValid(Date month){
        if(month == null) return ErrorCode.BUDGET_MONTH_IS_NULL;
        if(month.toLocalDate().getDayOfMonth() != 1) return ErrorCode.BUDGET_MONTH_INCORRECT_FORMAT;

        return ErrorCode.OK;
    }

    public static ErrorCode isBudgetCategoryValid(Integer id){
        if(id == null) return ErrorCode.BUDGET_CATEGORY_IS_NULL;

        return ErrorCode.OK;
    }

    public static ErrorCode isBudgetAmountValid(Double amount){
        if(amount == null) return ErrorCode.BUDGET_AMOUNT_IS_NULL;
        if(amount <= MIN_AMOUNT_VALUE || amount > MAX_AMOUNT_VALUE) return ErrorCode.BUDGET_AMOUNT_OUT_OF_BOUNDS;
        if(BigDecimal.valueOf(amount).stripTrailingZeros().scale() > MAX_NUMBER_OF_DECIMAL_PLACES) return ErrorCode.BUDGET_AMOUNT_INVALID_DECIMAL_PLACES;

        return ErrorCode.OK;
    }
}
