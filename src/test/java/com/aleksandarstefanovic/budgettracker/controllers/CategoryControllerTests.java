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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.CategoryRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.CategoryRepo.Result;
import com.aleksandarstefanovic.budgettracker.models.Category;

@WebMvcTest(CategoryController.class)
class CategoryControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepo categoryRepo;

    @Test
    void insertsCategory() throws Exception {
        when(categoryRepo.insertCategory(any(Category.class))).thenReturn(ErrorCode.OK);

        mockMvc.perform(post("/categories/insertCategory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Food","color":"#12AB34"}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        verify(categoryRepo).insertCategory(any(Category.class));
    }

    @Test
    void returnsBadRequestWhenCategoryInsertFailsValidation() throws Exception {
        when(categoryRepo.insertCategory(any(Category.class)))
                .thenReturn(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT);

        mockMvc.perform(post("/categories/insertCategory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Food","color":"red"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT.getDescription()));
    }

    @Test
    void updatesCategory() throws Exception {
        when(categoryRepo.updateCategory(any(Category.class))).thenReturn(ErrorCode.OK);

        mockMvc.perform(put("/categories/updateCategory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":1,"name":"Food","color":"#12AB34"}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        verify(categoryRepo).updateCategory(any(Category.class));
    }

    @Test
    void deletesCategory() throws Exception {
        when(categoryRepo.deleteCategory(1)).thenReturn(ErrorCode.OK);

        mockMvc.perform(delete("/categories/deleteCategory/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        verify(categoryRepo).deleteCategory(1);
    }

    @Test
    void getsCategories() throws Exception {
        when(categoryRepo.getCategories())
                .thenReturn(new Result(List.of(new Category(1, "Food", "#12AB34")), ErrorCode.OK));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[0].color").value("#12AB34"));

        verify(categoryRepo).getCategories();
    }
}
