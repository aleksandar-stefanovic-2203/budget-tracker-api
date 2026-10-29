package com.aleksandarstefanovic.budgettracker.db.dao.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.repo.CategoryRepo.Result;
import com.aleksandarstefanovic.budgettracker.models.Category;

class CategoryRepoTests {
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
    void insertsCategoryTrimmingNameAndBindingSqlParameters() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1);
            Category category = new Category(null, "  Food  ", "#12AB34");

            assertEquals(ErrorCode.OK, new CategoryRepo().insertCategory(category));

            assertEquals("Food", category.getName());
            verify(fixture.connection).prepareStatement("INSERT INTO category (name, color) VALUES (?, ?)");
            verify(fixture.statement).setString(1, "Food");
            verify(fixture.statement).setString(2, "#12AB34");
            verify(fixture.statement).executeUpdate();
        }
    }

    @Test
    void insertValidatesNameAndColorBeforeExecutingSql() throws Exception {
        try (Fixture fixture = new Fixture()) {
            CategoryRepo repo = new CategoryRepo();

            assertEquals(ErrorCode.CATEGORY_NAME_IS_NULL,
                    repo.insertCategory(new Category(null, null, "#12AB34")));
            assertEquals(ErrorCode.CATEGORY_NAME_INVALID_LENGTH,
                    repo.insertCategory(new Category(null, "", "#12AB34")));
            assertEquals(ErrorCode.CATEGORY_COLOR_IS_NULL,
                    repo.insertCategory(new Category(null, "Food", null)));
            assertEquals(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT,
                    repo.insertCategory(new Category(null, "Food", "red")));

            verify(fixture.statement, never()).executeUpdate();
        }
    }

    @Test
    void mapsDuplicateCategoryNameToConflictError() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenThrow(new SQLException("duplicate", null, 2627));

            assertEquals(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS,
                    new CategoryRepo().insertCategory(new Category(null, "Food", "#12AB34")));
        }
    }

    @Test
    void mapsInsertSqlFailureToServiceUnavailable() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenThrow(new SQLException("database error"));

            assertEquals(ErrorCode.SERVICE_UNAVAILABLE,
                    new CategoryRepo().insertCategory(new Category(null, "Food", "#12AB34")));
        }
    }

    @Test
    void updatesCategoryTrimmingNameAndBindingSqlParameters() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1);
            Category category = new Category(7, "  Food  ", "#12AB34");

            assertEquals(ErrorCode.OK, new CategoryRepo().updateCategory(category));

            assertEquals("Food", category.getName());
            verify(fixture.connection).prepareStatement("UPDATE category SET name = ?, color = ? WHERE id = ?");
            verify(fixture.statement).setString(1, "Food");
            verify(fixture.statement).setString(2, "#12AB34");
            verify(fixture.statement).setInt(3, 7);
        }
    }

    @Test
    void updateValidatesIdNameAndColorBeforeExecutingSql() throws Exception {
        try (Fixture fixture = new Fixture()) {
            CategoryRepo repo = new CategoryRepo();

            assertEquals(ErrorCode.CATEGORY_ID_IS_NULL,
                    repo.updateCategory(new Category(null, "Food", "#12AB34")));
            assertEquals(ErrorCode.CATEGORY_ID_OUT_OF_BOUNDS,
                    repo.updateCategory(new Category(0, "Food", "#12AB34")));
            assertEquals(ErrorCode.CATEGORY_NAME_IS_NULL,
                    repo.updateCategory(new Category(1, null, "#12AB34")));
            assertEquals(ErrorCode.CATEGORY_COLOR_INVALID_FORMAT,
                    repo.updateCategory(new Category(1, "Food", "red")));

            verify(fixture.statement, never()).executeUpdate();
        }
    }

    @Test
    void updateReturnsNotFoundWhenNoCategoryWasChanged() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(0);

            assertEquals(ErrorCode.CATEGORY_DOES_NOT_EXIST,
                    new CategoryRepo().updateCategory(new Category(7, "Food", "#12AB34")));
        }
    }

    @Test
    void mapsUpdateSqlErrors() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenThrow(new SQLException("duplicate", null, 2627))
                    .thenThrow(new SQLException("database error"));
            CategoryRepo repo = new CategoryRepo();
            Category category = new Category(7, "Food", "#12AB34");

            assertEquals(ErrorCode.CATEGORY_NAME_ALREADY_EXISTS, repo.updateCategory(category));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.updateCategory(category));
        }
    }

    @Test
    void deletesCategoryAndBindsId() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate()).thenReturn(1);

            assertEquals(ErrorCode.OK, new CategoryRepo().deleteCategory(7));

            verify(fixture.connection).prepareStatement("DELETE FROM category WHERE id = ?");
            verify(fixture.statement).setInt(1, 7);
        }
    }

    @Test
    void deleteMapsNotFoundForeignKeyAndDatabaseErrors() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeUpdate())
                    .thenReturn(0)
                    .thenThrow(new SQLException("referenced by transfer", null, 547))
                    .thenThrow(new SQLException("database error"));
            CategoryRepo repo = new CategoryRepo();

            assertEquals(ErrorCode.CATEGORY_DOES_NOT_EXIST, repo.deleteCategory(7));
            assertEquals(ErrorCode.CATEGORY_TRANSACTIONS_EXIST, repo.deleteCategory(7));
            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, repo.deleteCategory(7));
        }
    }

    @Test
    void getsCategoriesFromResultSet() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ResultSet resultSet = mock(ResultSet.class);
            when(fixture.statement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getInt("id")).thenReturn(7);
            when(resultSet.getString("name")).thenReturn("Food");
            when(resultSet.getString("color")).thenReturn("#12AB34");

            Result result = new CategoryRepo().getCategories();

            assertEquals(ErrorCode.OK, result.errCode());
            assertEquals(1, result.list().size());
            assertEquals(7, result.list().get(0).getId());
            assertEquals("Food", result.list().get(0).getName());
            assertEquals("#12AB34", result.list().get(0).getColor());
            verify(fixture.connection).prepareStatement("SELECT * FROM category ORDER BY name");
        }
    }

    @Test
    void getCategoriesReturnsServiceUnavailableWhenSqlFails() throws Exception {
        try (Fixture fixture = new Fixture()) {
            when(fixture.statement.executeQuery()).thenThrow(new SQLException("database error"));

            Result result = new CategoryRepo().getCategories();

            assertEquals(ErrorCode.SERVICE_UNAVAILABLE, result.errCode());
            assertEquals(null, result.list());
        }
    }
}
