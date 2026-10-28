package com.aleksandarstefanovic.budgettracker.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aleksandarstefanovic.budgettracker.db.dao.*;
import com.aleksandarstefanovic.budgettracker.models.Category;


@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = "http://localhost:8080/")
public class CategoryController {
    
    @PostMapping("/insertCategory")
    public ResponseEntity<String> insertCategory(@RequestBody Category category) {
        ErrorCode errCode = new CategoryRepo().insertCategory(category);
        return APIResponse.fromErrorCode(errCode);
    }

    @PutMapping("/updateCategory")
    public ResponseEntity<String> updateCategory(@RequestBody Category category) {
        ErrorCode errCode = new CategoryRepo().updateCategory(category);
        return APIResponse.fromErrorCode(errCode);
    }

    @DeleteMapping("/deleteCategory/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable int id) {
        ErrorCode errCode = new CategoryRepo().deleteCategory(id);
        return APIResponse.fromErrorCode(errCode);
    }

    @GetMapping("")
    public ResponseEntity<List<Category>> getCategories() {
        List<Category> categories = new CategoryRepo().getCategories();

        return APIResponse.fromList(categories);
    }
    
}
