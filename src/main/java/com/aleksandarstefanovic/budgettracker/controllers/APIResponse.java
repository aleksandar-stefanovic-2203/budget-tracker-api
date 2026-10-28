package com.aleksandarstefanovic.budgettracker.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;

public final class APIResponse {
    private APIResponse() {}

    public static ResponseEntity<String> fromErrorCode(ErrorCode errCode){
        return ResponseEntity.status(errCode.getStatus()).body(errCode.getDescription());
    }

    public static <T> ResponseEntity<List<T>> fromList(List<T> list) {
    if (list == null) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();

    return ResponseEntity.ok(list);
}
}
