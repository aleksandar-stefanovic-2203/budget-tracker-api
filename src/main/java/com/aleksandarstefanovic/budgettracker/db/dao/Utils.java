package com.aleksandarstefanovic.budgettracker.db.dao;

import java.util.regex.Pattern;

public class Utils {
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    public static ErrorCode isCategoryNameValid(String name){
        if(name == null) return ErrorCode.CATEGORY_NAME_IS_NULL;
        if(!name.equals(name.strip())) return ErrorCode.CATEGORY_NAME_NOT_STRIPPED;
        if(name.length() < 1 || name.length() > 30) return ErrorCode.CATEGORY_NAME_INVALID_LENGTH;

        return ErrorCode.NO_ERRORS;
    }

    public static ErrorCode isColorValid(String color){
        if(color == null) return ErrorCode.CATEGORY_COLOR_IS_NULL;
        if(!HEX_COLOR.matcher(color).matches()) return ErrorCode.CATEGORY_COLOR_INVALID_FORMAT;

        return ErrorCode.NO_ERRORS;
    }
}
