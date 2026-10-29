package com.aleksandarstefanovic.budgettracker.db.dao.validation;

import java.math.BigDecimal;
import java.sql.Date;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;

public final class TransferValidator {
    private static final double MAX_AMOUNT_VALUE = 1_000_000.00;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_CATEGORY_NAME_LENGTH = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private TransferValidator() {}

    public static ErrorCode isIDValid(Integer id) {
        if (id == null) return ErrorCode.TRANSACTION_ID_IS_NULL;
        if (id <= 0) return ErrorCode.TRANSACTION_ID_OUT_OF_BOUNDS;
        return ErrorCode.OK;
    }

    public static ErrorCode isTypeValid(String type) {
        if (type == null) return ErrorCode.TRANSACTION_TYPE_IS_NULL;
        if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
            return ErrorCode.TRANSACTION_TYPE_INVALID;
        }
        return ErrorCode.OK;
    }

    public static ErrorCode isAmountValid(Double amount) {
        if (amount == null) return ErrorCode.TRANSACTION_AMOUNT_IS_NULL;
        if (!Double.isFinite(amount) || amount <= 0 || amount > MAX_AMOUNT_VALUE) {
            return ErrorCode.TRANSACTION_AMOUNT_OUT_OF_BOUNDS;
        }
        if (BigDecimal.valueOf(amount).stripTrailingZeros().scale() > 2) {
            return ErrorCode.TRANSACTION_AMOUNT_INVALID_DECIMAL_PLACES;
        }
        return ErrorCode.OK;
    }

    public static ErrorCode isDateValid(Date date) {
        return date == null ? ErrorCode.TRANSACTION_DATE_IS_NULL : ErrorCode.OK;
    }

    public static ErrorCode isMonthValid(Date month) {
        if (month == null || month.toLocalDate().getDayOfMonth() != 1) {
            return ErrorCode.TRANSACTION_MONTH_INVALID_FORMAT;
        }
        return ErrorCode.OK;
    }

    public static ErrorCode isDescriptionValid(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            return ErrorCode.TRANSACTION_DESCRIPTION_INVALID_LENGTH;
        }
        return ErrorCode.OK;
    }

    public static ErrorCode isCategoryIDValid(Integer categoryId) {
        if (categoryId == null) return ErrorCode.TRANSACTION_CATEGORY_IS_NULL;
        if (categoryId <= 0) return ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST;
        return ErrorCode.OK;
    }

    public static ErrorCode isPageValid(Integer page) {
        if (page == null || page < 1) return ErrorCode.TRANSACTION_PAGE_INVALID;
        return ErrorCode.OK;
    }

    public static ErrorCode isPageSizeValid(Integer pageSize) {
        if (pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return ErrorCode.TRANSACTION_PAGE_SIZE_INVALID;
        }
        return ErrorCode.OK;
    }

    public static ErrorCode isDateRangeValid(Date startDate, Date endDate) {
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            return ErrorCode.TRANSACTION_DATE_RANGE_INVALID;
        }
        return ErrorCode.OK;
    }

    public static ErrorCode isCategoryNameFilterValid(String categoryName) {
        if (categoryName != null && categoryName.length() > MAX_CATEGORY_NAME_LENGTH) {
            return ErrorCode.TRANSACTION_CATEGORY_NAME_INVALID_LENGTH;
        }
        return ErrorCode.OK;
    }
}
