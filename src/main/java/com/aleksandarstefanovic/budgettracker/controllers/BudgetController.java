package com.aleksandarstefanovic.budgettracker.controllers;

import java.sql.Date;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo.Result;
import com.aleksandarstefanovic.budgettracker.models.Budget;

@RestController 
@RequestMapping("/budgets")
@CrossOrigin(origins = "http://localhost:8080/")
public class BudgetController {
    
    @PostMapping("/insertBudget")
    public ResponseEntity<String> insertBudget(@RequestBody Budget budget) {
        ErrorCode errCode = new BudgetRepo().insertBudget(budget);
        return APIResponse.fromErrorCode(errCode);
    }

    @PutMapping("/updateBudget")
    public ResponseEntity<String> updateBudget(@RequestBody Budget budget) {
        ErrorCode errCode = new BudgetRepo().updateBudget(budget);
        return APIResponse.fromErrorCode(errCode);
    }

    @DeleteMapping("/deleteBudget/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Integer id) {
        ErrorCode errCode = new BudgetRepo().deleteBudget(id);
        return APIResponse.fromErrorCode(errCode);
    }

    @GetMapping("")
    public ResponseEntity<List<Budget>> getCategories(@RequestParam Date month) {
        Result result = new BudgetRepo().getBudgetByMonth(month);

        return APIResponse.fromList(result.list(), result.errCode());
    }
}
