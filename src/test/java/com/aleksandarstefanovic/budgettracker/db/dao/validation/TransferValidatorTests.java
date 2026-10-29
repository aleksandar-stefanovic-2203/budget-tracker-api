package com.aleksandarstefanovic.budgettracker.db.dao.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;

import org.junit.jupiter.api.Test;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;

class TransferValidatorTests {
    @Test
    void validatesTransactionId() {
        assertEquals(ErrorCode.TRANSACTION_ID_IS_NULL, TransferValidator.isIDValid(null));
        assertEquals(ErrorCode.TRANSACTION_ID_OUT_OF_BOUNDS, TransferValidator.isIDValid(0));
        assertEquals(ErrorCode.TRANSACTION_ID_OUT_OF_BOUNDS, TransferValidator.isIDValid(-1));
        assertEquals(ErrorCode.OK, TransferValidator.isIDValid(1));
    }

    @Test
    void validatesType() {
        assertEquals(ErrorCode.TRANSACTION_TYPE_IS_NULL, TransferValidator.isTypeValid(null));
        assertEquals(ErrorCode.TRANSACTION_TYPE_INVALID, TransferValidator.isTypeValid("income"));
        assertEquals(ErrorCode.TRANSACTION_TYPE_INVALID, TransferValidator.isTypeValid("TRANSFER"));
        assertEquals(ErrorCode.OK, TransferValidator.isTypeValid("INCOME"));
        assertEquals(ErrorCode.OK, TransferValidator.isTypeValid("EXPENSE"));
    }

    @Test
    void validatesAmount() {
        assertEquals(ErrorCode.TRANSACTION_AMOUNT_IS_NULL, TransferValidator.isAmountValid(null));
        assertEquals(ErrorCode.TRANSACTION_AMOUNT_OUT_OF_BOUNDS, TransferValidator.isAmountValid(0.0));
        assertEquals(ErrorCode.TRANSACTION_AMOUNT_OUT_OF_BOUNDS, TransferValidator.isAmountValid(-0.01));
        assertEquals(ErrorCode.TRANSACTION_AMOUNT_OUT_OF_BOUNDS, TransferValidator.isAmountValid(1_000_000.01));
        assertEquals(ErrorCode.TRANSACTION_AMOUNT_OUT_OF_BOUNDS, TransferValidator.isAmountValid(Double.NaN));
        assertEquals(ErrorCode.TRANSACTION_AMOUNT_OUT_OF_BOUNDS, TransferValidator.isAmountValid(Double.POSITIVE_INFINITY));
        assertEquals(
                ErrorCode.TRANSACTION_AMOUNT_INVALID_DECIMAL_PLACES,
                TransferValidator.isAmountValid(10.123));
        assertEquals(ErrorCode.OK, TransferValidator.isAmountValid(0.01));
        assertEquals(ErrorCode.OK, TransferValidator.isAmountValid(1_000_000.0));
        assertEquals(ErrorCode.OK, TransferValidator.isAmountValid(10.12));
    }

    @Test
    void validatesDateDescriptionAndCategory() {
        assertEquals(ErrorCode.TRANSACTION_DATE_IS_NULL, TransferValidator.isDateValid(null));
        assertEquals(ErrorCode.OK, TransferValidator.isDateValid(Date.valueOf("2026-10-29")));
        assertEquals(ErrorCode.OK, TransferValidator.isDescriptionValid(null));
        assertEquals(ErrorCode.OK, TransferValidator.isDescriptionValid("a".repeat(200)));
        assertEquals(
                ErrorCode.TRANSACTION_DESCRIPTION_INVALID_LENGTH,
                TransferValidator.isDescriptionValid("a".repeat(201)));
        assertEquals(ErrorCode.TRANSACTION_CATEGORY_IS_NULL, TransferValidator.isCategoryIDValid(null));
        assertEquals(
                ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST,
                TransferValidator.isCategoryIDValid(0));
        assertEquals(ErrorCode.OK, TransferValidator.isCategoryIDValid(1));
    }

    @Test
    void validatesMonthlySummaryMonth() {
        assertEquals(ErrorCode.TRANSACTION_MONTH_INVALID_FORMAT, TransferValidator.isMonthValid(null));
        assertEquals(
                ErrorCode.TRANSACTION_MONTH_INVALID_FORMAT,
                TransferValidator.isMonthValid(Date.valueOf("2026-10-02")));
        assertEquals(ErrorCode.OK, TransferValidator.isMonthValid(Date.valueOf("2026-10-01")));
    }

    @Test
    void validatesPaginationAndFilters() {
        assertEquals(ErrorCode.TRANSACTION_PAGE_INVALID, TransferValidator.isPageValid(null));
        assertEquals(ErrorCode.TRANSACTION_PAGE_INVALID, TransferValidator.isPageValid(0));
        assertEquals(ErrorCode.OK, TransferValidator.isPageValid(1));
        assertEquals(ErrorCode.TRANSACTION_PAGE_SIZE_INVALID, TransferValidator.isPageSizeValid(null));
        assertEquals(ErrorCode.TRANSACTION_PAGE_SIZE_INVALID, TransferValidator.isPageSizeValid(0));
        assertEquals(ErrorCode.TRANSACTION_PAGE_SIZE_INVALID, TransferValidator.isPageSizeValid(101));
        assertEquals(ErrorCode.OK, TransferValidator.isPageSizeValid(100));
        assertEquals(
                ErrorCode.TRANSACTION_DATE_RANGE_INVALID,
                TransferValidator.isDateRangeValid(Date.valueOf("2026-10-02"), Date.valueOf("2026-10-01")));
        assertEquals(
                ErrorCode.OK,
                TransferValidator.isDateRangeValid(Date.valueOf("2026-10-01"), Date.valueOf("2026-10-01")));
        assertEquals(
                ErrorCode.TRANSACTION_CATEGORY_NAME_INVALID_LENGTH,
                TransferValidator.isCategoryNameFilterValid("a".repeat(31)));
    }
}
