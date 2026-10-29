package com.aleksandarstefanovic.budgettracker.db.dao.repo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.validation.BudgetValidator;
import com.aleksandarstefanovic.budgettracker.models.Budget;

@Repository
public class BudgetRepo {
    public ErrorCode insertBudget(Budget budget) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("INSERT INTO budget (month, category_id, amount) VALUES (?, ?, ?)")){
                ErrorCode errCode = BudgetValidator.isBudgetMonthValid(budget.getMonth());
                if(errCode != ErrorCode.OK) return errCode;

                errCode = BudgetValidator.isBudgetCategoryValid(budget.getCategory_id());
                if(errCode != ErrorCode.OK) return errCode;

                errCode = BudgetValidator.isBudgetAmountValid(budget.getAmount());
                if(errCode != ErrorCode.OK) return errCode;

                stm.setDate(1, budget.getMonth());
                stm.setInt(2, budget.getCategory_id());
                stm.setDouble(3, budget.getAmount());

                stm.executeUpdate();
                return ErrorCode.OK;

        } catch (SQLException e) {
            if(e.getErrorCode() == 2627) return ErrorCode.BUDGET_EXISTS;
            if(e.getErrorCode() == 547) return ErrorCode.BUDGET_CATEGORY_DOES_NOT_EXIST;

            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public ErrorCode updateBudget(Budget budget) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("UPDATE budget SET month = ?, category_id = ?, amount = ? WHERE id = ?")){
                ErrorCode errCode = BudgetValidator.isBudgetIDValid(budget.getId());
                if(errCode != ErrorCode.OK) return errCode;
                
                errCode = BudgetValidator.isBudgetMonthValid(budget.getMonth());
                if(errCode != ErrorCode.OK) return errCode;

                errCode = BudgetValidator.isBudgetCategoryValid(budget.getCategory_id());
                if(errCode != ErrorCode.OK) return errCode;

                errCode = BudgetValidator.isBudgetAmountValid(budget.getAmount());
                if(errCode != ErrorCode.OK) return errCode;

                stm.setDate(1, budget.getMonth());
                stm.setInt(2, budget.getCategory_id());
                stm.setDouble(3, budget.getAmount());
                stm.setInt(4, budget.getId());

                int rowCount = stm.executeUpdate();
                return rowCount == 1 ? ErrorCode.OK : ErrorCode.BUDGET_DOES_NOT_EXIST;

        } catch (SQLException e) {
            if(e.getErrorCode() == 2627) return ErrorCode.BUDGET_EXISTS;
            if(e.getErrorCode() == 547) return ErrorCode.BUDGET_CATEGORY_DOES_NOT_EXIST;

            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public ErrorCode deleteBudget(Integer id) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("DELETE FROM budget WHERE id = ?")){
                ErrorCode errCode = BudgetValidator.isBudgetIDValid(id);
                if(errCode != ErrorCode.OK) return errCode;

                stm.setInt(1, id);

                int rowCount = stm.executeUpdate();
                return rowCount == 1 ? ErrorCode.OK : ErrorCode.BUDGET_DOES_NOT_EXIST;

        } catch (SQLException e) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public record Result(List<Budget> list, ErrorCode errCode) {}

    public Result getBudgetByMonth(Date month){
        try(Connection con = DB.source().getConnection();
            PreparedStatement stm = con.prepareStatement("SELECT * FROM budget WHERE month = ?")){            
            
            ErrorCode errCode = BudgetValidator.isBudgetMonthValid(month);
            if(errCode != ErrorCode.OK) return new Result(null, errCode);

            stm.setDate(1, month);

            ResultSet rs = stm.executeQuery();
            List<Budget> budgets = new ArrayList<>();

            while(rs.next()){
                budgets.add(new Budget(rs.getInt("id"), rs.getDate("month"), rs.getInt("category_id"), rs.getDouble("amount")));
            }

            return new Result(budgets, ErrorCode.OK);

        } catch (SQLException e){
            return new Result(null, ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
