package com.aleksandarstefanovic.budgettracker.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo.Result;
import com.aleksandarstefanovic.budgettracker.models.Budget;

@WebMvcTest(BudgetController.class)
class BudgetControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetRepo budgetRepo;

    @Test
    void insertsBudget() throws Exception {
        when(budgetRepo.insertBudget(any(Budget.class))).thenReturn(ErrorCode.OK);

        mockMvc.perform(post("/budgets/insertBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"2026-10-01","category_id":3,"amount":250.50}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        verify(budgetRepo).insertBudget(any(Budget.class));
    }

    @Test
    void returnsConflictWhenBudgetAlreadyExists() throws Exception {
        when(budgetRepo.insertBudget(any(Budget.class))).thenReturn(ErrorCode.BUDGET_EXISTS);

        mockMvc.perform(post("/budgets/insertBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"2026-10-01","category_id":3,"amount":250.50}
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().string(ErrorCode.BUDGET_EXISTS.getDescription()));
    }

    @Test
    void updatesBudget() throws Exception {
        when(budgetRepo.updateBudget(any(Budget.class))).thenReturn(ErrorCode.OK);

        mockMvc.perform(put("/budgets/updateBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":2,"month":"2026-10-01","category_id":3,"amount":250.50}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        verify(budgetRepo).updateBudget(any(Budget.class));
    }

    @Test
    void deletesBudget() throws Exception {
        when(budgetRepo.deleteBudget(2)).thenReturn(ErrorCode.OK);

        mockMvc.perform(delete("/budgets/deleteBudget/2"))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        verify(budgetRepo).deleteBudget(2);
    }

    @Test
    void getsBudgetsForMonth() throws Exception {
        Date month = Date.valueOf("2026-10-01");
        when(budgetRepo.getBudgetByMonth(month))
                .thenReturn(new Result(List.of(new Budget(2, month, 3, 250.50)), ErrorCode.OK));

        mockMvc.perform(get("/budgets").param("month", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].category_id").value(3))
                .andExpect(jsonPath("$[0].amount").value(250.50));

        verify(budgetRepo).getBudgetByMonth(month);
    }
}
