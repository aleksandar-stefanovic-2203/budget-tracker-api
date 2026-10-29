package com.aleksandarstefanovic.budgettracker.db.dao.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;

import org.junit.jupiter.api.Test;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;

class BudgetValidatorTests {
    @Test
    void validatesBudgetId() {
        assertEquals(ErrorCode.BUDGET_ID_IS_NULL, BudgetValidator.isBudgetIDValid(null));
        assertEquals(ErrorCode.BUDGET_ID_OUT_OF_BOUNDS, BudgetValidator.isBudgetIDValid(0));
        assertEquals(ErrorCode.BUDGET_ID_OUT_OF_BOUNDS, BudgetValidator.isBudgetIDValid(-1));
        assertEquals(ErrorCode.OK, BudgetValidator.isBudgetIDValid(1));
    }

    @Test
    void validatesBudgetMonth() {
        assertEquals(ErrorCode.BUDGET_MONTH_IS_NULL, BudgetValidator.isBudgetMonthValid(null));
        assertEquals(
                ErrorCode.BUDGET_MONTH_INCORRECT_FORMAT,
                BudgetValidator.isBudgetMonthValid(Date.valueOf("2026-10-02")));
        assertEquals(ErrorCode.OK, BudgetValidator.isBudgetMonthValid(Date.valueOf("2026-10-01")));
    }

    @Test
    void validatesBudgetCategory() {
        assertEquals(ErrorCode.BUDGET_CATEGORY_IS_NULL, BudgetValidator.isBudgetCategoryValid(null));
        assertEquals(ErrorCode.OK, BudgetValidator.isBudgetCategoryValid(1));
    }

    @Test
    void validatesBudgetAmount() {
        assertEquals(ErrorCode.BUDGET_AMOUNT_IS_NULL, BudgetValidator.isBudgetAmountValid(null));
        assertEquals(ErrorCode.BUDGET_AMOUNT_OUT_OF_BOUNDS, BudgetValidator.isBudgetAmountValid(0.0));
        assertEquals(ErrorCode.BUDGET_AMOUNT_OUT_OF_BOUNDS, BudgetValidator.isBudgetAmountValid(-0.01));
        assertEquals(ErrorCode.BUDGET_AMOUNT_OUT_OF_BOUNDS, BudgetValidator.isBudgetAmountValid(1_000_000.01));
        assertEquals(
                ErrorCode.BUDGET_AMOUNT_INVALID_DECIMAL_PLACES,
                BudgetValidator.isBudgetAmountValid(1.234));
        assertEquals(ErrorCode.OK, BudgetValidator.isBudgetAmountValid(0.01));
        assertEquals(ErrorCode.OK, BudgetValidator.isBudgetAmountValid(1_000_000.0));
        assertEquals(ErrorCode.OK, BudgetValidator.isBudgetAmountValid(12.34));
    }
}
