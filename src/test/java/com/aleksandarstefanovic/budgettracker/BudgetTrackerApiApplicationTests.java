package com.aleksandarstefanovic.budgettracker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo.Result;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.CategoryRepo;
import com.aleksandarstefanovic.budgettracker.models.Budget;
import com.aleksandarstefanovic.budgettracker.models.Category;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class BudgetTrackerApiApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BudgetRepo budgetRepo;

	@MockitoBean
	private CategoryRepo categoryRepo;

	@Test
	void contextLoads() {
	}

	@Test
	void categoryEndpointReturnsJsonFromApplicationContext() throws Exception {
		when(categoryRepo.getCategories())
				.thenReturn(new CategoryRepo.Result(List.of(new Category(1, "Food", "#12AB34")), ErrorCode.OK));

		mockMvc.perform(get("/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Food"))
				.andExpect(jsonPath("$[0].color").value("#12AB34"));

		verify(categoryRepo).getCategories();
	}

	@Test
	void budgetEndpointDeserializesRequestAndReturnsRepositoryResult() throws Exception {
		when(budgetRepo.insertBudget(any(Budget.class))).thenReturn(ErrorCode.OK);

		mockMvc.perform(post("/budgets/insertBudget")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"month":"2026-10-01","category_id":3,"amount":250.50}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().string(ErrorCode.OK.getDescription()));

		org.mockito.ArgumentCaptor<Budget> budgetCaptor = org.mockito.ArgumentCaptor.forClass(Budget.class);
		verify(budgetRepo).insertBudget(budgetCaptor.capture());
		Budget receivedBudget = budgetCaptor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals(
				Date.valueOf("2026-10-01").toLocalDate(), receivedBudget.getMonth().toLocalDate());
		org.junit.jupiter.api.Assertions.assertEquals(3, receivedBudget.getCategory_id());
		org.junit.jupiter.api.Assertions.assertEquals(250.50, receivedBudget.getAmount());
	}

	@Test
	void budgetEndpointRejectsUnparseableMonthBeforeCallingRepository() throws Exception {
		mockMvc.perform(get("/budgets").param("month", "not-a-date"))
				.andExpect(status().isBadRequest());

		org.mockito.Mockito.verifyNoInteractions(budgetRepo);
	}
}
