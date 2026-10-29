package com.aleksandarstefanovic.budgettracker.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo;

@WebMvcTest(BudgetController.class)
@Import(BudgetControllerIntegrationTests.RepoConfiguration.class)
class BudgetControllerIntegrationTests {
    private static final Date MONTH = Date.valueOf("2026-10-01");

    @TestConfiguration
    static class RepoConfiguration {
        @Bean
        BudgetRepo budgetRepo() {
            return new BudgetRepo();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private MockedStatic<DB> db;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        db = mockStatic(DB.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        db.when(DB::source).thenReturn(dataSource);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void postValidatesRequestAndPersistsBudget() throws Exception {
        when(statement.executeUpdate()).thenReturn(1);

        mockMvc.perform(post("/budgets/insertBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"2026-10-01","category_id":3,"amount":250.50}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        org.mockito.Mockito.verify(connection).prepareStatement(
                "INSERT INTO budget (month, category_id, amount) VALUES (?, ?, ?)");
        org.mockito.Mockito.verify(statement).setDate(
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.argThat(date ->
                        date.toLocalDate().equals(MONTH.toLocalDate())));
        org.mockito.Mockito.verify(statement).setInt(2, 3);
        org.mockito.Mockito.verify(statement).setDouble(3, 250.50);
        org.mockito.Mockito.verify(statement).executeUpdate();
    }

    @Test
    void postRejectsInvalidAmountBeforeExecutingSql() throws Exception {
        mockMvc.perform(post("/budgets/insertBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"month":"2026-10-01","category_id":3,"amount":1.234}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.BUDGET_AMOUNT_INVALID_DECIMAL_PLACES.getDescription()));

        org.mockito.Mockito.verify(statement, org.mockito.Mockito.never()).executeUpdate();
    }

    @Test
    void postMapsDuplicateAndMissingCategorySqlErrors() throws Exception {
        when(statement.executeUpdate())
                .thenThrow(new SQLException("duplicate budget", null, 2627))
                .thenThrow(new SQLException("missing category", null, 547));
        String request = """
                {"month":"2026-10-01","category_id":3,"amount":250.50}
                """;

        mockMvc.perform(post("/budgets/insertBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isConflict())
                .andExpect(content().string(ErrorCode.BUDGET_EXISTS.getDescription()));

        mockMvc.perform(post("/budgets/insertBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(ErrorCode.BUDGET_CATEGORY_DOES_NOT_EXIST.getDescription()));
    }

    @Test
    void putUpdatesBudgetAndMapsMissingRow() throws Exception {
        when(statement.executeUpdate()).thenReturn(1, 0);
        String request = """
                {"id":7,"month":"2026-10-01","category_id":3,"amount":250.50}
                """;

        mockMvc.perform(put("/budgets/updateBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));
        org.mockito.Mockito.verify(statement).setInt(4, 7);

        mockMvc.perform(put("/budgets/updateBudget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.BUDGET_DOES_NOT_EXIST.getDescription()));
    }

    @Test
    void deleteValidatesIdAndMapsDatabaseFailure() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("database error"));

        mockMvc.perform(delete("/budgets/deleteBudget/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.BUDGET_ID_OUT_OF_BOUNDS.getDescription()));

        mockMvc.perform(delete("/budgets/deleteBudget/7"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(ErrorCode.SERVICE_UNAVAILABLE.getDescription()));
    }

    @Test
    void getReadsBudgetRowsAndReturnsJson() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("id")).thenReturn(7);
        when(resultSet.getDate("month")).thenReturn(MONTH);
        when(resultSet.getInt("category_id")).thenReturn(3);
        when(resultSet.getDouble("amount")).thenReturn(250.50);

        mockMvc.perform(get("/budgets").param("month", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].category_id").value(3))
                .andExpect(jsonPath("$[0].amount").value(250.50));

        org.mockito.Mockito.verify(connection).prepareStatement("SELECT * FROM budget WHERE month = ?");
        org.mockito.Mockito.verify(statement).setDate(
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.argThat(date ->
                        date.toLocalDate().equals(MONTH.toLocalDate())));
    }

    @Test
    void getRejectsInvalidMonthWithoutQueryingDatabase() throws Exception {
        mockMvc.perform(get("/budgets").param("month", "2026-10-02"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verify(statement, org.mockito.Mockito.never()).executeQuery();
    }

    @Test
    void getMapsSqlFailureToServiceUnavailable() throws Exception {
        when(statement.executeQuery()).thenThrow(new SQLException("database error"));

        mockMvc.perform(get("/budgets").param("month", "2026-10-01"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
    }
}
