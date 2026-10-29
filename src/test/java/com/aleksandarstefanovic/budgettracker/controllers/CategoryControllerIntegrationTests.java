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
import com.aleksandarstefanovic.budgettracker.db.dao.repo.CategoryRepo;

@WebMvcTest(CategoryController.class)
@Import(CategoryControllerIntegrationTests.RepoConfiguration.class)
class CategoryControllerIntegrationTests {
    @TestConfiguration
    static class RepoConfiguration {
        @Bean
        CategoryRepo categoryRepo() {
            return new CategoryRepo();
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
    void postValidatesTrimsAndPersistsCategory() throws Exception {
        when(statement.executeUpdate()).thenReturn(1);

        mockMvc.perform(post("/categories/insertCategory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"  Food  ","color":"#12AB34"}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        org.mockito.Mockito.verify(connection)
                .prepareStatement("INSERT INTO category (name, color) VALUES (?, ?)");
        org.mockito.Mockito.verify(statement).setString(1, "Food");
        org.mockito.Mockito.verify(statement).setString(2, "#12AB34");
        org.mockito.Mockito.verify(statement).executeUpdate();
    }

    @Test
    void postRejectsInvalidCategoryWithoutWritingToDatabase() throws Exception {
        mockMvc.perform(post("/categories/insertCategory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","color":"bad"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.CATEGORY_NAME_INVALID_LENGTH.getDescription()));

        org.mockito.Mockito.verify(statement, org.mockito.Mockito.never()).executeUpdate();
    }

    @Test
    void postMapsDuplicateNameToConflict() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("duplicate name", null, 2627));

        mockMvc.perform(post("/categories/insertCategory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Food","color":"#12AB34"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().string(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS.getDescription()));
    }

    @Test
    void putMapsMissingCategoryToBadRequest() throws Exception {
        when(statement.executeUpdate()).thenReturn(0);

        mockMvc.perform(put("/categories/updateCategory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":7,"name":"Food","color":"#12AB34"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.CATEGORY_DOES_NOT_EXIST.getDescription()));

        org.mockito.Mockito.verify(statement).setInt(3, 7);
    }

    @Test
    void deleteMapsTransactionReferenceToConflict() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("referenced by transfer", null, 547));

        mockMvc.perform(delete("/categories/deleteCategory/7"))
                .andExpect(status().isConflict())
                .andExpect(content().string(ErrorCode.CATEGORY_TRANSACTIONS_EXIST.getDescription()));
    }

    @Test
    void getReadsCategoriesFromJdbcAndReturnsJson() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("id")).thenReturn(7);
        when(resultSet.getString("name")).thenReturn("Food");
        when(resultSet.getString("color")).thenReturn("#12AB34");

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[0].color").value("#12AB34"));

        org.mockito.Mockito.verify(connection).prepareStatement("SELECT * FROM category ORDER BY name");
        org.mockito.Mockito.verify(statement).executeQuery();
    }

    @Test
    void getMapsDatabaseFailureToServiceUnavailable() throws Exception {
        when(statement.executeQuery()).thenThrow(new SQLException("database error"));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
    }
}
