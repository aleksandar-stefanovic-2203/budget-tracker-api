package com.aleksandarstefanovic.budgettracker.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aleksandarstefanovic.budgettracker.db.dao.*;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.CategoryRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.CategoryRepo.Result;
import com.aleksandarstefanovic.budgettracker.models.Category;


@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = "http://localhost:8080/")
public class CategoryController {

    private final CategoryRepo categoryRepo;

    public CategoryController(CategoryRepo categoryRepo) {
        this.categoryRepo = categoryRepo;
    }
    
    @PostMapping("/insertCategory")
    public ResponseEntity<String> insertCategory(@RequestBody Category category) {
        ErrorCode errCode = categoryRepo.insertCategory(category);
        return APIResponse.fromErrorCode(errCode);
    }

    @PutMapping("/updateCategory")
    public ResponseEntity<String> updateCategory(@RequestBody Category category) {
        ErrorCode errCode = categoryRepo.updateCategory(category);
        return APIResponse.fromErrorCode(errCode);
    }

    @DeleteMapping("/deleteCategory/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable int id) {
        ErrorCode errCode = categoryRepo.deleteCategory(id);
        return APIResponse.fromErrorCode(errCode);
    }

    @GetMapping("")
    public ResponseEntity<List<Category>> getCategories() {
        Result result = categoryRepo.getCategories();

        return APIResponse.fromList(result.list(), result.errCode());
    }
    
}
