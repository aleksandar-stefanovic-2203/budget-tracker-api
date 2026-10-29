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
import org.springframework.mock.web.MockMultipartFile;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo;
import com.aleksandarstefanovic.budgettracker.services.TransferCsvImportService;

@WebMvcTest(TransferController.class)
@Import(TransferControllerIntegrationTests.RepoConfiguration.class)
class TransferControllerIntegrationTests {
    private static final Date DATE = Date.valueOf("2026-10-29");

    @TestConfiguration
    static class RepoConfiguration {
        @Bean
        TransferRepo transferRepo() {
            return new TransferRepo();
        }

        @Bean
        TransferCsvImportService transferCsvImportService(TransferRepo transferRepo) {
            return new TransferCsvImportService(transferRepo);
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
    void postInsertsValidExpenseAndAllowsMissingDescription() throws Exception {
        when(statement.executeUpdate()).thenReturn(1);

        mockMvc.perform(post("/transactions/insertTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"EXPENSE","amount":12.34,"date":"2026-10-29","categoryId":4}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        org.mockito.Mockito.verify(connection).prepareStatement(
                "INSERT INTO transfer (type, amount, date, description, category_id) VALUES (?, ?, ?, ?, ?)");
        org.mockito.Mockito.verify(statement).setString(1, "EXPENSE");
        org.mockito.Mockito.verify(statement).setDouble(2, 12.34);
        org.mockito.Mockito.verify(statement).setString(4, null);
        org.mockito.Mockito.verify(statement).setInt(5, 4);
        org.mockito.Mockito.verify(statement).executeUpdate();
    }

    @Test
    void postRejectsMissingInvalidTypeAndDescriptionTooLong() throws Exception {
        mockMvc.perform(post("/transactions/insertTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"amount":12.34,"date":"2026-10-29","categoryId":4}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_TYPE_IS_NULL.getDescription()));

        mockMvc.perform(post("/transactions/insertTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"OTHER","amount":12.34,"date":"2026-10-29","categoryId":4}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_TYPE_INVALID.getDescription()));

        mockMvc.perform(post("/transactions/insertTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"INCOME","amount":12.34,"date":"2026-10-29","description":"%s","categoryId":4}
                        """.formatted("x".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_DESCRIPTION_INVALID_LENGTH.getDescription()));

        org.mockito.Mockito.verify(statement, org.mockito.Mockito.never()).executeUpdate();
    }

    @Test
    void postMapsMissingCategoryAndDatabaseErrors() throws Exception {
        when(statement.executeUpdate())
                .thenThrow(new SQLException("missing category", null, 547))
                .thenThrow(new SQLException("database error"));
        String request = """
                {"type":"INCOME","amount":12.34,"date":"2026-10-29","categoryId":4}
                """;

        mockMvc.perform(post("/transactions/insertTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST.getDescription()));

        mockMvc.perform(post("/transactions/insertTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(ErrorCode.SERVICE_UNAVAILABLE.getDescription()));
    }

    @Test
    void csvImportContinuesAfterInvalidAndDatabaseRejectedRows() throws Exception {
        when(statement.executeUpdate())
                .thenReturn(1)
                .thenThrow(new SQLException("missing category", null, 547))
                .thenReturn(1);
        String csv = """
                type,amount,date,description,categoryId
                EXPENSE,12.34,2026-10-29,Lunch,4
                EXPENSE,not-a-number,2026-10-29,Invalid amount,4
                INCOME,100.00,2026-10-29,Unknown category,99
                INCOME,1500.00,2026-10-30,Salary,4
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(4))
                .andExpect(jsonPath("$.importedRows").value(2))
                .andExpect(jsonPath("$.failedRows").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(3))
                .andExpect(jsonPath("$.errors[0].code").value("TRANSACTION_AMOUNT_INVALID_FORMAT"))
                .andExpect(jsonPath("$.errors[1].row").value(4))
                .andExpect(jsonPath("$.errors[1].code").value("TRANSACTION_CATEGORY_DOES_NOT_EXIST"));

        org.mockito.Mockito.verify(statement, org.mockito.Mockito.times(3)).executeUpdate();
    }

    @Test
    void csvImportSupportsQuotedCommasAndEscapedQuotes() throws Exception {
        when(statement.executeUpdate()).thenReturn(1);
        String csv = """
                type,amount,date,description,categoryId
                EXPENSE,12.34,2026-10-29,\"Lunch, team \"\"A\"\"\",4
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(0));

        org.mockito.Mockito.verify(statement).setString(4, "Lunch, team \"A\"");
    }

    @Test
    void csvImportAcceptsBomAndOptionalDescriptionWithReorderedColumns() throws Exception {
        when(statement.executeUpdate()).thenReturn(1);
        String csv = "\uFEFFcategoryId,date,amount,type\n4,2026-10-29,3.50,EXPENSE\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedRows").value(1))
                .andExpect(jsonPath("$.failedRows").value(0));

        org.mockito.Mockito.verify(statement).setString(4, null);
    }

    @Test
    void csvImportRejectsMalformedFileBeforeInsertingAnyRows() throws Exception {
        String csv = """
                type,amount,date,description,categoryId
                EXPENSE,12.34,2026-10-29,"unterminated description,4
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CSV_FILE_INVALID"));

        org.mockito.Mockito.verify(connection, org.mockito.Mockito.never()).prepareStatement(anyString());
    }

    @Test
    void csvImportRejectsInvalidHeaderAndReportsWrongColumnCountPerRow() throws Exception {
        MockMultipartFile invalidHeader = new MockMultipartFile(
                "file", "bad.csv", "text/csv",
                "type,amount,date\nEXPENSE,12.34,2026-10-29\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(invalidHeader))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CSV_FILE_INVALID"));

        MockMultipartFile invalidRow = new MockMultipartFile(
                "file", "bad-row.csv", "text/csv",
                ("type,amount,date,description,categoryId\n"
                        + "EXPENSE,12.34,2026-10-29,Lunch,4,unexpected\n")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(invalidRow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.importedRows").value(0))
                .andExpect(jsonPath("$.errors[0].row").value(2))
                .andExpect(jsonPath("$.errors[0].code").value("TRANSACTION_CSV_ROW_INVALID"));

        org.mockito.Mockito.verify(connection, org.mockito.Mockito.never()).prepareStatement(anyString());
    }

    @Test
    void csvImportRejectsEmptyAndOversizedFiles() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CSV_FILE_INVALID"));

        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file", "large.csv", "text/csv", new byte[5 * 1024 * 1024 + 1]);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/transactions/import")
                        .file(oversizedFile))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CSV_FILE_TOO_LARGE"));
    }

    @Test
    void putUpdatesTransferAndReturnsNotFoundWhenNoRowMatches() throws Exception {
        when(statement.executeUpdate()).thenReturn(1, 0);
        String request = """
                {"id":9,"type":"INCOME","amount":12.34,"date":"2026-10-29","description":"salary","categoryId":4}
                """;

        mockMvc.perform(put("/transactions/updateTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        mockMvc.perform(put("/transactions/updateTransaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isNotFound())
                .andExpect(content().string(ErrorCode.TRANSACTION_DOES_NOT_EXIST.getDescription()));

        org.mockito.Mockito.verify(statement, org.mockito.Mockito.times(2)).setInt(6, 9);
    }

    @Test
    void deleteValidatesIdAndDeletesTransfer() throws Exception {
        when(statement.executeUpdate()).thenReturn(1);

        mockMvc.perform(delete("/transactions/deleteTransaction/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_ID_OUT_OF_BOUNDS.getDescription()));

        mockMvc.perform(delete("/transactions/deleteTransaction/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(ErrorCode.OK.getDescription()));

        org.mockito.Mockito.verify(statement).setInt(1, 9);
    }

    @Test
    void deleteReturnsNotFoundWhenTransactionDoesNotExist() throws Exception {
        when(statement.executeUpdate()).thenReturn(0);

        mockMvc.perform(delete("/transactions/deleteTransaction/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(ErrorCode.TRANSACTION_DOES_NOT_EXIST.getDescription()));
    }

    @Test
    void getByIdReturnsTransferAndNotFoundResponse() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("id")).thenReturn(9);
        when(resultSet.getString("type")).thenReturn("EXPENSE");
        when(resultSet.getDouble("amount")).thenReturn(12.34);
        when(resultSet.getDate("date")).thenReturn(DATE);
        when(resultSet.getString("description")).thenReturn("Lunch");
        when(resultSet.getInt("category_id")).thenReturn(4);

        mockMvc.perform(get("/transactions/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(12.34))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.categoryId").value(4));

        when(resultSet.next()).thenReturn(false);
        mockMvc.perform(get("/transactions/8"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(ErrorCode.TRANSACTION_DOES_NOT_EXIST.getDescription()));
    }

    @Test
    void getByIdMapsDatabaseFailureToServiceUnavailable() throws Exception {
        when(statement.executeQuery()).thenThrow(new SQLException("database error"));

        mockMvc.perform(get("/transactions/9"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(ErrorCode.SERVICE_UNAVAILABLE.getDescription()));
    }

    @Test
    void getAllSupportsPaginationAndCombinedFilters() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        mockMvc.perform(get("/transactions")
                .param("page", "2")
                .param("pageSize", "10")
                .param("startDate", "2026-10-01")
                .param("endDate", "2026-10-29")
                .param("categoryName", "Food")
                .param("type", "EXPENSE")
                .param("description", "lunch"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        org.mockito.Mockito.verify(connection).prepareStatement(
                "SELECT t.id, t.type, t.amount, t.date, t.description, t.category_id "
                        + "FROM transfer t JOIN category c ON c.id = t.category_id "
                        + "WHERE 1 = 1 AND t.date >= ? AND t.date <= ? AND c.name LIKE ? "
                        + "AND t.type = ? AND t.description LIKE ? "
                        + "ORDER BY t.date DESC, t.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        org.mockito.Mockito.verify(statement).setString(3, "%Food%");
        org.mockito.Mockito.verify(statement).setString(4, "EXPENSE");
        org.mockito.Mockito.verify(statement).setString(5, "%lunch%");
        org.mockito.Mockito.verify(statement).setLong(6, 10);
        org.mockito.Mockito.verify(statement).setInt(7, 10);
    }

    @Test
    void getAllRejectsInvalidPageSizeDateRangeCategoryAndType() throws Exception {
        mockMvc.perform(get("/transactions").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_PAGE_SIZE_INVALID.getDescription()));
        mockMvc.perform(get("/transactions")
                .param("startDate", "2026-10-29")
                .param("endDate", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_DATE_RANGE_INVALID.getDescription()));
        mockMvc.perform(get("/transactions").param("type", "OTHER"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_TYPE_INVALID.getDescription()));
        mockMvc.perform(get("/transactions").param("categoryName", "x".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_CATEGORY_NAME_INVALID_LENGTH.getDescription()));
        mockMvc.perform(get("/transactions").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_PAGE_INVALID.getDescription()));
        mockMvc.perform(get("/transactions").param("description", "x".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(ErrorCode.TRANSACTION_DESCRIPTION_INVALID_LENGTH.getDescription()));

        org.mockito.Mockito.verify(connection, org.mockito.Mockito.never()).prepareStatement(anyString());
    }

    @Test
    void getAllMapsDatabaseFailureToServiceUnavailable() throws Exception {
        when(statement.executeQuery()).thenThrow(new SQLException("database error"));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(ErrorCode.SERVICE_UNAVAILABLE.getDescription()));
    }
}
