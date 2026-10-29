package com.aleksandarstefanovic.budgettracker.db.dao.validation;

import java.util.regex.Pattern;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;

public class CategoryValidator {
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final Integer MIN_NAME_LENGTH = 1;
    private static final Integer MAX_NAME_LENGTH = 30;

    public static ErrorCode isCategoryIDValid(Integer id){
        if(id == null) return ErrorCode.CATEGORY_ID_IS_NULL;
        if(id <= 0) return ErrorCode.CATEGORY_ID_OUT_OF_BOUNDS;

        return ErrorCode.OK;
    }

    public static ErrorCode isCategoryNameValid(String name){
        if(name == null) return ErrorCode.CATEGORY_NAME_IS_NULL;
        if(name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) return ErrorCode.CATEGORY_NAME_INVALID_LENGTH;

        return ErrorCode.OK;
    }

    public static ErrorCode isColorValid(String color){
        if(color == null) return ErrorCode.CATEGORY_COLOR_IS_NULL;
        if(!HEX_COLOR.matcher(color).matches()) return ErrorCode.CATEGORY_COLOR_INVALID_FORMAT;

        return ErrorCode.OK;
    }
}
