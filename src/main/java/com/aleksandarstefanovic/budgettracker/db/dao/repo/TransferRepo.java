package com.aleksandarstefanovic.budgettracker.db.dao.repo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.validation.TransferValidator;
import com.aleksandarstefanovic.budgettracker.models.Transfer;

@Repository
public class TransferRepo {
    private static final String INSERT_TRANSFER_SQL =
            "INSERT INTO transfer (type, amount, date, description, category_id) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_COLUMNS =
            "SELECT t.id, t.type, t.amount, t.date, t.description, t.category_id "
                    + "FROM transfer t JOIN category c ON c.id = t.category_id";

    public ErrorCode insertTransfer(Transfer transfer) {
        try (Connection connection = DB.source().getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_TRANSFER_SQL)) {
            ErrorCode errorCode = validateTransfer(transfer, false);
            if (errorCode != ErrorCode.OK) return errorCode;

            bindTransfer(statement, transfer, false);
            statement.executeUpdate();
            return ErrorCode.OK;
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 547) return ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST;
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public CsvImportSession openCsvImportSession() throws SQLException {
        Connection connection = DB.source().getConnection();
        try {
            return new CsvImportSession(connection);
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public static final class CsvImportSession implements AutoCloseable {
        private final Connection connection;
        private final PreparedStatement insertStatement;
        private final Map<String, Integer> categoryIds;

        private CsvImportSession(Connection connection) throws SQLException {
            this.connection = connection;
            this.categoryIds = new HashMap<>();
            try (PreparedStatement categoryStatement = connection.prepareStatement("SELECT id, name FROM category");
                    ResultSet categories = categoryStatement.executeQuery()) {
                while (categories.next()) {
                    categoryIds.putIfAbsent(
                            categories.getString("name").toLowerCase(Locale.ROOT),
                            categories.getInt("id"));
                }
            }
            this.insertStatement = connection.prepareStatement(INSERT_TRANSFER_SQL);
        }

        public ErrorCode insertTransfer(String categoryName, Transfer transfer) {
            if (categoryName == null || categoryName.isBlank()) return ErrorCode.TRANSACTION_CATEGORY_IS_NULL;
            Integer categoryId = categoryIds.get(categoryName.strip().toLowerCase(Locale.ROOT));
            if (categoryId == null) return ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST;

            transfer.setCategoryId(categoryId);
            ErrorCode errorCode = validateTransfer(transfer, false);
            if (errorCode != ErrorCode.OK) return errorCode;

            try {
                bindTransfer(insertStatement, transfer, false);
                insertStatement.executeUpdate();
                return ErrorCode.OK;
            } catch (SQLException exception) {
                if (exception.getErrorCode() == 547) return ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST;
                return ErrorCode.SERVICE_UNAVAILABLE;
            }
        }

        @Override
        public void close() throws SQLException {
            try {
                insertStatement.close();
            } finally {
                connection.close();
            }
        }
    }

    public ErrorCode updateTransfer(Transfer transfer) {
        try (Connection connection = DB.source().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE transfer SET type = ?, amount = ?, date = ?, description = ?, category_id = ? "
                                + "WHERE id = ?")) {
            ErrorCode errorCode = validateTransfer(transfer, true);
            if (errorCode != ErrorCode.OK) return errorCode;

            bindTransfer(statement, transfer, true);
            int rowCount = statement.executeUpdate();
            return rowCount == 1 ? ErrorCode.OK : ErrorCode.TRANSACTION_DOES_NOT_EXIST;
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 547) return ErrorCode.TRANSACTION_CATEGORY_DOES_NOT_EXIST;
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public ErrorCode deleteTransfer(Integer id) {
        try (Connection connection = DB.source().getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM transfer WHERE id = ?")) {
            ErrorCode errorCode = TransferValidator.isIDValid(id);
            if (errorCode != ErrorCode.OK) return errorCode;

            statement.setInt(1, id);
            int rowCount = statement.executeUpdate();
            return rowCount == 1 ? ErrorCode.OK : ErrorCode.TRANSACTION_DOES_NOT_EXIST;
        } catch (SQLException exception) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public record SingleResult(Transfer transfer, ErrorCode errCode) {}

    public SingleResult getTransferByID(Integer id) {
        ErrorCode errorCode = TransferValidator.isIDValid(id);
        if (errorCode != ErrorCode.OK) return new SingleResult(null, errorCode);

        try (Connection connection = DB.source().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        SELECT_COLUMNS + " WHERE t.id = ?")) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return new SingleResult(null, ErrorCode.TRANSACTION_DOES_NOT_EXIST);
            }
            return new SingleResult(readTransfer(resultSet), ErrorCode.OK);
        } catch (SQLException exception) {
            return new SingleResult(null, ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    public record Result(List<Transfer> list, ErrorCode errCode) {}

    public record MonthlySummary(
            String month,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal balance,
            long transactionCount,
            List<CategoryMonthlySummary> categories) {}

    public record MonthlySummaryResult(MonthlySummary summary, ErrorCode errCode) {}

    public record CategoryMonthlySummary(
            String categoryName,
            BigDecimal spent,
            BigDecimal budget,
            BigDecimal left) {}

    public MonthlySummaryResult getMonthlySummary(Date month) {
        ErrorCode errorCode = TransferValidator.isMonthValid(month);
        if (errorCode != ErrorCode.OK) return new MonthlySummaryResult(null, errorCode);

        LocalDate start = month.toLocalDate();
        Date nextMonth = Date.valueOf(start.plusMonths(1));
        String totalsSql = "SELECT "
                + "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income, "
                + "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expenses, "
                + "COUNT_BIG(*) AS transaction_count "
                + "FROM transfer WHERE date >= ? AND date < ?";
        String categoriesSql = "SELECT c.name AS category_name, "
                + "COALESCE(expenses.spent, 0) AS spent, COALESCE(b.amount, 0) AS budget "
                + "FROM category c "
                + "LEFT JOIN (SELECT category_id, SUM(amount) AS spent FROM transfer "
                + "WHERE type = 'EXPENSE' AND date >= ? AND date < ? GROUP BY category_id) expenses "
                + "ON expenses.category_id = c.id "
                + "LEFT JOIN budget b ON b.category_id = c.id AND b.month = ? "
                + "WHERE expenses.category_id IS NOT NULL OR b.category_id IS NOT NULL "
                + "ORDER BY COALESCE(expenses.spent, 0) DESC, c.name ASC";

        try (Connection connection = DB.source().getConnection();
                PreparedStatement totalsStatement = connection.prepareStatement(totalsSql);
                PreparedStatement categoriesStatement = connection.prepareStatement(categoriesSql)) {
            totalsStatement.setDate(1, month);
            totalsStatement.setDate(2, nextMonth);
            ResultSet totalsResult = totalsStatement.executeQuery();
            if (!totalsResult.next()) return new MonthlySummaryResult(null, ErrorCode.SERVICE_UNAVAILABLE);

            BigDecimal totalIncome = totalsResult.getBigDecimal("total_income");
            BigDecimal totalExpenses = totalsResult.getBigDecimal("total_expenses");
            categoriesStatement.setDate(1, month);
            categoriesStatement.setDate(2, nextMonth);
            categoriesStatement.setDate(3, month);
            ResultSet categoriesResult = categoriesStatement.executeQuery();
            List<CategoryMonthlySummary> categories = new ArrayList<>();
            while (categoriesResult.next()) {
                BigDecimal spent = categoriesResult.getBigDecimal("spent");
                BigDecimal budget = categoriesResult.getBigDecimal("budget");
                categories.add(new CategoryMonthlySummary(
                        categoriesResult.getString("category_name"),
                        spent,
                        budget,
                        budget.subtract(spent)));
            }
            MonthlySummary summary = new MonthlySummary(
                    start.toString(),
                    totalIncome,
                    totalExpenses,
                    totalIncome.subtract(totalExpenses),
                    totalsResult.getLong("transaction_count"),
                    List.copyOf(categories));
            return new MonthlySummaryResult(summary, ErrorCode.OK);
        } catch (SQLException exception) {
            return new MonthlySummaryResult(null, ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    public Result getTransfers(
            Integer page,
            Integer pageSize,
            Date startDate,
            Date endDate,
            String categoryName,
            String type,
            String description) {
        ErrorCode errorCode = TransferValidator.isPageValid(page);
        if (errorCode != ErrorCode.OK) return new Result(null, errorCode);
        errorCode = TransferValidator.isPageSizeValid(pageSize);
        if (errorCode != ErrorCode.OK) return new Result(null, errorCode);
        errorCode = TransferValidator.isDateRangeValid(startDate, endDate);
        if (errorCode != ErrorCode.OK) return new Result(null, errorCode);
        errorCode = TransferValidator.isCategoryNameFilterValid(categoryName);
        if (errorCode != ErrorCode.OK) return new Result(null, errorCode);
        if (type != null) {
            errorCode = TransferValidator.isTypeValid(type);
            if (errorCode != ErrorCode.OK) return new Result(null, errorCode);
        }
        errorCode = TransferValidator.isDescriptionValid(description);
        if (errorCode != ErrorCode.OK) return new Result(null, errorCode);

        StringBuilder whereClause = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendFilters(whereClause, parameters, startDate, endDate, categoryName, type, description);
        long offset = ((long) page - 1) * pageSize;

        try (Connection connection = DB.source().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        SELECT_COLUMNS + whereClause + " ORDER BY t.date DESC, t.id DESC "
                                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")) {
            bindParameters(statement, parameters);
            statement.setLong(parameters.size() + 1, offset);
            statement.setInt(parameters.size() + 2, pageSize);

            ResultSet resultSet = statement.executeQuery();
            List<Transfer> transfers = new ArrayList<>();
            while (resultSet.next()) {
                transfers.add(readTransfer(resultSet));
            }
            return new Result(transfers, ErrorCode.OK);
        } catch (SQLException exception) {
            return new Result(null, ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private static ErrorCode validateTransfer(Transfer transfer, boolean includeId) {
        if (includeId) {
            ErrorCode errorCode = TransferValidator.isIDValid(transfer.getId());
            if (errorCode != ErrorCode.OK) return errorCode;
        }

        ErrorCode errorCode = TransferValidator.isTypeValid(transfer.getType());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isAmountValid(transfer.getAmount());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isDateValid(transfer.getDate());
        if (errorCode != ErrorCode.OK) return errorCode;
        errorCode = TransferValidator.isDescriptionValid(transfer.getDescription());
        if (errorCode != ErrorCode.OK) return errorCode;
        return TransferValidator.isCategoryIDValid(transfer.getCategoryId());
    }

    private static void bindTransfer(PreparedStatement statement, Transfer transfer, boolean includeId)
            throws SQLException {
        statement.setString(1, transfer.getType());
        statement.setDouble(2, transfer.getAmount());
        statement.setDate(3, transfer.getDate());
        statement.setString(4, transfer.getDescription());
        statement.setInt(5, transfer.getCategoryId());
        if (includeId) statement.setInt(6, transfer.getId());
    }

    private static void appendFilters(
            StringBuilder whereClause,
            List<Object> parameters,
            Date startDate,
            Date endDate,
            String categoryName,
            String type,
            String description) {
        if (startDate != null) {
            whereClause.append(" AND t.date >= ?");
            parameters.add(startDate);
        }
        if (endDate != null) {
            whereClause.append(" AND t.date <= ?");
            parameters.add(endDate);
        }
        if (categoryName != null && !categoryName.isBlank()) {
            whereClause.append(" AND c.name LIKE ?");
            parameters.add("%" + categoryName.strip() + "%");
        }
        if (type != null) {
            whereClause.append(" AND t.type = ?");
            parameters.add(type);
        }
        if (description != null && !description.isBlank()) {
            whereClause.append(" AND t.description LIKE ?");
            parameters.add("%" + description.strip() + "%");
        }
    }

    private static void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object parameter = parameters.get(index);
            int position = index + 1;
            if (parameter instanceof Date date) {
                statement.setDate(position, date);
            } else {
                statement.setString(position, (String) parameter);
            }
        }
    }

    private static Transfer readTransfer(ResultSet resultSet) throws SQLException {
        return new Transfer(
                resultSet.getInt("id"),
                resultSet.getString("type"),
                resultSet.getDouble("amount"),
                resultSet.getDate("date"),
                resultSet.getString("description"),
                resultSet.getInt("category_id"));
    }
}
