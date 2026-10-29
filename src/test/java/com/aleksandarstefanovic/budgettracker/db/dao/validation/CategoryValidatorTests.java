package com.aleksandarstefanovic.budgettracker.db.dao.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;

class CategoryValidatorTests {
    @Test
    void validatesCategoryId() {
        assertEquals(ErrorCode.CATEGORY_ID_IS_NULL, CategoryValidator.isCategoryIDValid(null));
        assertEquals(ErrorCode.CATEGORY_ID_OUT_OF_BOUNDS, CategoryValidator.isCategoryIDValid(0));
        assertEquals(ErrorCode.CATEGORY_ID_OUT_OF_BOUNDS, CategoryValidator.isCategoryIDValid(-1));
        assertEquals(ErrorCode.OK, CategoryValidator.isCategoryIDValid(1));
    }

    @Test
    void validatesCategoryName() {
        assertEquals(ErrorCode.CATEGORY_NAME_IS_NULL, CategoryValidator.isCategoryNameValid(null));
        assertEquals(ErrorCode.CATEGORY_NAME_INVALID_LENGTH, CategoryValidator.isCategoryNameValid(""));
        assertEquals(ErrorCode.OK, CategoryValidator.isCategoryNameValid("A"));
        assertEquals(ErrorCode.OK, CategoryValidator.isCategoryNameValid("a".repeat(30)));
        assertEquals(ErrorCode.CATEGORY_NAME_INVALID_LENGTH, CategoryValidator.isCategoryNameValid("a".repeat(31)));
    }

    @Test
    void validatesCategoryColor() {
        assertEquals(ErrorCode.CATEGORY_COLOR_IS_NULL, CategoryValidator.isColorValid(null));
        assertEquals(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT, CategoryValidator.isColorValid("123456"));
        assertEquals(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT, CategoryValidator.isColorValid("#12345"));
        assertEquals(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT, CategoryValidator.isColorValid("#1234567"));
        assertEquals(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT, CategoryValidator.isColorValid("#12GG56"));
        assertEquals(ErrorCode.OK, CategoryValidator.isColorValid("#12aB34"));
    }
}
