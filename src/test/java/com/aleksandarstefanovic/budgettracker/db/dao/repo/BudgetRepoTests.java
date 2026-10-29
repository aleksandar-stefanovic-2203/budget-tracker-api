package com.aleksandarstefanovic.budgettracker.db.dao.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.BudgetRepo.Result;
import com.aleksandarstefanovic.budgettracker.models.Budget;

class BudgetRepoTests {
    private static final Date MONTH = Date.valueOf("2026-10-01");

    private static class Fixture implements AutoCloseable {
        private final DataSource dataSource = mock(DataSource.class);
        private final Connection connection = mock(Connection.class);
        private final PreparedStatement statement = mock(PreparedStatement.class);
        private final MockedStatic<DB> db = mockStatic(DB.class);

        Fixture() throws SQLException {
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            db.when(DB::source).thenReturn(dataSource);
        }

        @Override
        public void close() {
            db.close();
        }
    }

    @Test
    void insertsBudgetAndBindsParameters() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1);

            assertEquals(ErrorCode.OK,
                    new BudgetRepo().insertBudget(new Budget(null, MONTH, 3, 250.50)));

            verify(fixture.connection).prepareStatement(
                    "INSERT INTO budget (month, category_id, amount) VALUES (?, ?, ?)");
            verify(fixture.statement).setDate(1, MONTH);
            verify(fixture.statement).setInt(2, 3);
            verify(fixture.statement).setDouble(3, 250.50);
            verify(fixture.statement).executeUpdate();
        }
    }

    @Test
    void insertRejectsInvalidMonthCategoryAndAmount() throws Exception {
        try (Fixture fixture = new Fixture()) {
            BudgetRepo repo = new BudgetRepo();

            assertEquals(ErrorCode.BUDGET_MONTH_IS_NULL,
                    repo.insertBudget(new Budget(null, null, 3, 250.50)));
            assertEquals(ErrorCode.BUDGET_MONTH_INCORRECT_FORMAT,
                    repo.insertBudget(new Budget(null, Date.valueOf("2026-10-02"), 3, 250.50)));
            assertEquals(ErrorCode.BUDGET_CATEGORY_IS_NULL,
                    repo.insertBudget(new Budget(null, MONTH, null, 250.50)));
            assertEquals(ErrorCode.BUDGET_AMOUNT_IS_NULL,
                    repo.insertBudget(new Budget(null, MONTH, 3, null)));
            assertEquals(ErrorCode.BUDGET_AMOUNT_OUT_OF_BOUNDS,
                    repo.insertBudget(new Budget(null, MONTH, 3, 0.0)));
            assertEquals(ErrorCode.BUDGET_AMOUNT_INVALID_DECIMAL_PLACES,
                    repo.insertBudget(new Budget(null, MONTH, 3, 1.234)));

            verify(fixture.statement, never()).executeUpdate();
        }
    }

    @Test
    void insertMapsDuplicateMissingCategoryAndDatabaseErrors() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenThrow(new SQLException("duplicate budget", null, 2627))
                    .thenThrow(new SQLException("missing category", null, 547))
                    .thenThrow(new SQLException("database error"));
            BudgetRepo repo = new BudgetRepo();
            Budget budget = new Budget(null, MONTH, 3, 250.50);

            assertEquals(ErrorCode.BUDGET_EXISTS, repo.insertBudget(budget));
            assertEquals(ErrorCode.BUDGET_CATEGORY_DOES_NOT_EXIST, repo.insertBudget(budget));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.insertBudget(budget));
        }
    }

    @Test
    void updatesBudgetAndBindsParameters() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1);

            assertEquals(ErrorCode.OK,
                    new BudgetRepo().updateBudget(new Budget(7, MONTH, 3, 250.50)));

            verify(fixture.connection).prepareStatement(
                    "UPDATE budget SET month = ?, category_id = ?, amount = ? WHERE id = ?");
            verify(fixture.statement).setDate(1, MONTH);
            verify(fixture.statement).setInt(2, 3);
            verify(fixture.statement).setDouble(3, 250.50);
            verify(fixture.statement).setInt(4, 7);
        }
    }

    @Test
    void updateValidatesIdMonthCategoryAndAmount() throws Exception {
        try (Fixture fixture = new Fixture()) {
            BudgetRepo repo = new BudgetRepo();

            assertEquals(ErrorCode.BUDGET_ID_IS_NULL,
                    repo.updateBudget(new Budget(null, MONTH, 3, 250.50)));
            assertEquals(ErrorCode.BUDGET_ID_OUT_OF_BOUNDS,
                    repo.updateBudget(new Budget(0, MONTH, 3, 250.50)));
            assertEquals(ErrorCode.BUDGET_MONTH_IS_NULL,
                    repo.updateBudget(new Budget(7, null, 3, 250.50)));
            assertEquals(ErrorCode.BUDGET_CATEGORY_IS_NULL,
                    repo.updateBudget(new Budget(7, MONTH, null, 250.50)));
            assertEquals(ErrorCode.BUDGET_AMOUNT_IS_NULL,
                    repo.updateBudget(new Budget(7, MONTH, 3, null)));
            assertEquals(ErrorCode.BUDGET_AMOUNT_INVALID_DECIMAL_PLACES,
                    repo.updateBudget(new Budget(7, MONTH, 3, 1.234)));

            verify(fixture.statement, never()).executeUpdate();
        }
    }

    @Test
    void updateMapsNotFoundAndSqlErrors() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenReturn(0)
                    .thenThrow(new SQLException("duplicate budget", null, 2627))
                    .thenThrow(new SQLException("missing category", null, 547))
                    .thenThrow(new SQLException("database error"));
            BudgetRepo repo = new BudgetRepo();
            Budget budget = new Budget(7, MONTH, 3, 250.50);

            assertEquals(ErrorCode.BUDGET_DOES_NOT_EXIST, repo.updateBudget(budget));
            assertEquals(ErrorCode.BUDGET_EXISTS, repo.updateBudget(budget));
            assertEquals(ErrorCode.BUDGET_CATEGORY_DOES_NOT_EXIST, repo.updateBudget(budget));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.updateBudget(budget));
        }
    }

    @Test
    void deletesBudgetAndBindsId() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1);

            assertEquals(ErrorCode.OK, new BudgetRepo().deleteBudget(7));

            verify(fixture.connection).prepareStatement("DELETE FROM budget WHERE id = ?");
            verify(fixture.statement).setInt(1, 7);
        }
    }

    @Test
    void deleteValidatesIdAndMapsMissingRowAndSqlError() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenReturn(0)
                    .thenThrow(new SQLException("database error"));
            BudgetRepo repo = new BudgetRepo();

            assertEquals(ErrorCode.BUDGET_ID_IS_NULL, repo.deleteBudget(null));
            assertEquals(ErrorCode.BUDGET_ID_OUT_OF_BOUNDS, repo.deleteBudget(0));
            assertEquals(ErrorCode.BUDGET_DOES_NOT_EXIST, repo.deleteBudget(7));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.deleteBudget(7));
        }
    }

    @Test
    void getsBudgetsForMonthFromResultSet() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ResultSet resultSet = mock(ResultSet.class);
            when(fixture.statement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getInt("id")).thenReturn(7);
            when(resultSet.getDate("month")).thenReturn(MONTH);
            when(resultSet.getInt("category_id")).thenReturn(3);
            when(resultSet.getDouble("amount")).thenReturn(250.50);

            Result result = new BudgetRepo().getBudgetByMonth(MONTH);

            assertEquals(ErrorCode.OK, result.errCode());
            assertEquals(1, result.list().size());
            assertEquals(7, result.list().get(0).getId());
            assertEquals(MONTH, result.list().get(0).getMonth());
            assertEquals(3, result.list().get(0).getCategory_id());
            assertEquals(250.50, result.list().get(0).getAmount());
            verify(fixture.statement).setDate(1, MONTH);
        }
    }

    @Test
    void returnsEmptyListWhenNoBudgetsExistForMonth() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ResultSet resultSet = mock(ResultSet.class);
            when(fixture.statement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            Result result = new BudgetRepo().getBudgetByMonth(MONTH);

            assertEquals(ErrorCode.OK, result.errCode());
            assertEquals(0, result.list().size());
        }
    }

    @Test
    void getValidatesMonthAndMapsSqlFailure() throws Exception {
        try (Fixture fixture = new Fixture()) {
            BudgetRepo repo = new BudgetRepo();

            assertEquals(ErrorCode.BUDGET_MONTH_IS_NULL, repo.getBudgetByMonth(null).errCode());
            assertEquals(ErrorCode.BUDGET_MONTH_INCORRECT_FORMAT,
                    repo.getBudgetByMonth(Date.valueOf("2026-10-02")).errCode());
            verify(fixture.statement, never()).executeQuery();

            when(fixture.statement.executeQuery()).thenThrow(new SQLException("database error"));
            Result result = repo.getBudgetByMonth(MONTH);
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, result.errCode());
            assertNull(result.list());
        }
    }
}
