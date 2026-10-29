package com.aleksandarstefanovic.budgettracker.db.dao.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo.Result;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.TransferRepo.SingleResult;
import com.aleksandarstefanovic.budgettracker.models.Transfer;

class TransferRepoTests {
    private static final Date DATE = Date.valueOf("2026-10-29");

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
    void insertsTransferWithOptionalDescription() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1);
            Transfer transfer = new Transfer(null, "EXPENSE", 12.34, DATE, null, 4);

            assertEquals(ErrorCode.OK, new TransferRepo().insertTransfer(transfer));

            verify(fixture.connection).prepareStatement(
                    "INSERT INTO transfer (type, amount, date, description, category_id) VALUES (?, ?, ?, ?, ?)");
            verify(fixture.statement).setString(1, "EXPENSE");
            verify(fixture.statement).setDouble(2, 12.34);
            verify(fixture.statement).setDate(3, DATE);
            verify(fixture.statement).setString(4, null);
            verify(fixture.statement).setInt(5, 4);
            verify(fixture.statement).executeUpdate();
        }
    }

    @Test
    void insertValidatesFieldsBeforeSqlExecution() throws Exception {
        try (Fixture fixture = new Fixture()) {
            TransferRepo repo = new TransferRepo();
            assertEquals(ErrorCode.TRANSACTION_TYPE_IS_NULL,
                    repo.insertTransfer(new Transfer(null, null, 1.0, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_TYPE_INVALID,
                    repo.insertTransfer(new Transfer(null, "OTHER", 1.0, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_AMOUNT_IS_NULL,
                    repo.insertTransfer(new Transfer(null, "INCOME", null, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_DATE_IS_NULL,
                    repo.insertTransfer(new Transfer(null, "INCOME", 1.0, null, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_DESCRIPTION_INVALID_LENGTH,
                    repo.insertTransfer(new Transfer(null, "INCOME", 1.0, DATE, "x".repeat(201), 4)));
            assertEquals(ErrorCode.TRANSACTION_CATEGORY_IS_NULL,
                    repo.insertTransfer(new Transfer(null, "INCOME", 1.0, DATE, null, null)));

            verify(fixture.statement, never()).executeUpdate();
        }
    }

    @Test
    void insertMapsMissingCategoryAndDatabaseErrors() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenThrow(new SQLException("missing category", null, 547))
                    .thenThrow(new SQLException("database error"));
            Transfer transfer = new Transfer(null, "INCOME", 1.0, DATE, null, 4);
            TransferRepo repo = new TransferRepo();

            assertEquals(ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST, repo.insertTransfer(transfer));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.insertTransfer(transfer));
        }
    }

    @Test
    void updatesTransferAndDetectsMissingRow() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1, 0);
            TransferRepo repo = new TransferRepo();
            Transfer transfer = new Transfer(9, "INCOME", 12.34, DATE, "salary", 4);

            assertEquals(ErrorCode.OK, repo.updateTransfer(transfer));
            assertEquals(ErrorCode.TRANSACTION_DOES_NOT_EXIST, repo.updateTransfer(transfer));

            verify(fixture.connection, times(2)).prepareStatement(
                    "UPDATE transfer SET type = ?, amount = ?, date = ?, description = ?, category_id = ? WHERE id = ?");
            verify(fixture.statement, times(2)).setInt(6, 9);
        }
    }

    @Test
    void updateValidatesIdAndTransferFields() throws Exception {
        try (Fixture fixture = new Fixture()) {
            TransferRepo repo = new TransferRepo();
            assertEquals(ErrorCode.TRANSACTION_ID_IS_NULL,
                    repo.updateTransfer(new Transfer(null, "INCOME", 1.0, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_ID_OUT_OF_BOUNDS,
                    repo.updateTransfer(new Transfer(0, "INCOME", 1.0, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_TYPE_IS_NULL,
                    repo.updateTransfer(new Transfer(1, null, 1.0, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_TYPE_INVALID,
                    repo.updateTransfer(new Transfer(1, "OTHER", 1.0, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_AMOUNT_OUT_OF_BOUNDS,
                    repo.updateTransfer(new Transfer(1, "INCOME", 0.0, DATE, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_DATE_IS_NULL,
                    repo.updateTransfer(new Transfer(1, "INCOME", 1.0, null, null, 4)));
            assertEquals(ErrorCode.TRANSACTION_DESCRIPTION_INVALID_LENGTH,
                    repo.updateTransfer(new Transfer(1, "INCOME", 1.0, DATE, "x".repeat(201), 4)));
            assertEquals(ErrorCode.TRANSACTION_CATEGORY_IS_NULL,
                    repo.updateTransfer(new Transfer(1, "INCOME", 1.0, DATE, null, null)));
            verify(fixture.statement, never()).executeUpdate();
        }
    }

    @Test
    void updateMapsCategoryAndDatabaseErrors() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenThrow(new SQLException("missing category", null, 547))
                    .thenThrow(new SQLException("database error"));
            Transfer transfer = new Transfer(9, "INCOME", 1.0, DATE, null, 4);
            TransferRepo repo = new TransferRepo();

            assertEquals(ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST, repo.updateTransfer(transfer));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.updateTransfer(transfer));
        }
    }

    @Test
    void deletesTransferAndMapsMissingRowAndSqlError() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenReturn(1, 0)
                    .thenThrow(new SQLException("database error"));
            TransferRepo repo = new TransferRepo();

            assertEquals(ErrorCode.OK, repo.deleteTransfer(9));
            assertEquals(ErrorCode.TRANSACTION_DOES_NOT_EXIST, repo.deleteTransfer(9));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.deleteTransfer(9));
            assertEquals(ErrorCode.TRANSACTION_ID_IS_NULL, repo.deleteTransfer(null));
            assertEquals(ErrorCode.TRANSACTION_ID_OUT_OF_BOUNDS, repo.deleteTransfer(0));
        }
    }

    @Test
    void getsTransferByIdAndHandlesNotFound() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ResultSet resultSet = mock(ResultSet.class);
            when(fixture.statement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getInt("id")).thenReturn(9);
            when(resultSet.getString("type")).thenReturn("EXPENSE");
            when(resultSet.getDouble("amount")).thenReturn(12.34);
            when(resultSet.getDate("date")).thenReturn(DATE);
            when(resultSet.getString("description")).thenReturn("Lunch");
            when(resultSet.getInt("category_id")).thenReturn(4);
            TransferRepo repo = new TransferRepo();

            SingleResult found = repo.getTransferByID(9);
            assertEquals(ErrorCode.OK, found.errCode());
            assertEquals("EXPENSE", found.transfer().getType());
            assertEquals(12.34, found.transfer().getAmount());
            assertEquals("Lunch", found.transfer().getDescription());

            when(resultSet.next()).thenReturn(false);
            assertEquals(ErrorCode.TRANSACTION_DOES_NOT_EXIST, repo.getTransferByID(8).errCode());
        }
    }

    @Test
    void getByIdValidatesIdAndMapsDatabaseFailure() throws Exception {
        try (Fixture fixture = new Fixture()) {
            TransferRepo repo = new TransferRepo();
            assertEquals(ErrorCode.TRANSACTION_ID_IS_NULL, repo.getTransferByID(null).errCode());
            assertEquals(ErrorCode.TRANSACTION_ID_OUT_OF_BOUNDS, repo.getTransferByID(0).errCode());
            verify(fixture.connection, never()).prepareStatement(anyString());

            when(fixture.statement.executeQuery()).thenThrow(new SQLException("database error"));
            SingleResult result = repo.getTransferByID(9);
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, result.errCode());
            assertNull(result.transfer());
        }
    }

    @Test
    void searchesTransfersWithFiltersAndPagination() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ResultSet resultSet = mock(ResultSet.class);
            when(fixture.statement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            Result result = new TransferRepo().getTransfers(
                    2, 10, Date.valueOf("2026-10-01"), DATE, "Food", "EXPENSE", "lunch");

            assertEquals(ErrorCode.OK, result.errCode());
            assertEquals(0, result.list().size());
            verify(fixture.connection).prepareStatement(
                    "SELECT t.id, t.type, t.amount, t.date, t.description, t.category_id "
                            + "FROM transfer t JOIN category c ON c.id = t.category_id "
                            + "WHERE 1 = 1 AND t.date >= ? AND t.date <= ? AND c.name LIKE ? "
                            + "AND t.type = ? AND t.description LIKE ? "
                            + "ORDER BY t.date DESC, t.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
            verify(fixture.statement).setDate(1, Date.valueOf("2026-10-01"));
            verify(fixture.statement).setDate(2, DATE);
            verify(fixture.statement).setString(3, "%Food%");
            verify(fixture.statement).setString(4, "EXPENSE");
            verify(fixture.statement).setString(5, "%lunch%");
            verify(fixture.statement).setLong(6, 10);
            verify(fixture.statement).setInt(7, 10);
        }
    }

    @Test
    void getTransfersValidatesPaginationDateRangeAndFilters() throws Exception {
        try (Fixture fixture = new Fixture()) {
            TransferRepo repo = new TransferRepo();
            assertEquals(ErrorCode.TRANSACTION_PAGE_INVALID,
                    repo.getTransfers(0, 10, null, null, null, null, null).errCode());
            assertEquals(ErrorCode.TRANSACTION_PAGE_SIZE_INVALID,
                    repo.getTransfers(1, 101, null, null, null, null, null).errCode());
            assertEquals(ErrorCode.TRANSACTION_DATE_RANGE_INVALID,
                    repo.getTransfers(1, 10, DATE, Date.valueOf("2026-10-01"), null, null, null).errCode());
            assertEquals(ErrorCode.TRANSACTION_CATEGORY_NAME_INVALID_LENGTH,
                    repo.getTransfers(1, 10, null, null, "x".repeat(31), null, null).errCode());
            assertEquals(ErrorCode.TRANSACTION_TYPE_INVALID,
                    repo.getTransfers(1, 10, null, null, null, "OTHER", null).errCode());
            assertEquals(ErrorCode.TRANSACTION_DESCRIPTION_INVALID_LENGTH,
                    repo.getTransfers(1, 10, null, null, null, null, "x".repeat(201)).errCode());
            verify(fixture.connection, never()).prepareStatement(anyString());
        }
    }

    @Test
    void getTransfersMapsDatabaseFailure() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeQuery()).thenThrow(new SQLException("database error"));

            Result result = new TransferRepo().getTransfers(1, 20, null, null, null, null, null);

            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, result.errCode());
            assertNull(result.list());
        }
    }
}
