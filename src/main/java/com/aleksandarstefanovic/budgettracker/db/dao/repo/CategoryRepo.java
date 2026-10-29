package com.aleksandarstefanovic.budgettracker.db.dao.repo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.db.dao.ErrorCode;
import com.aleksandarstefanovic.budgettracker.db.dao.validation.CategoryValidator;
import com.aleksandarstefanovic.budgettracker.models.Category;

@Repository
public class CategoryRepo {
    public ErrorCode insertCategory(Category category) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("INSERT INTO category (name, color) VALUES (?, ?)")){
                if(category.getName() != null){
                    String strippedName = category.getName().strip();
                    category.setName(strippedName);
                }

                ErrorCode errCode = CategoryValidator.isCategoryNameValid(category.getName());
                if(errCode != ErrorCode.OK) return errCode;

                errCode = CategoryValidator.isColorValid(category.getColor());
                if(errCode != ErrorCode.OK) return errCode;

                stm.setString(1, category.getName());
                stm.setString(2, category.getColor());

                stm.executeUpdate();
                return ErrorCode.OK;

        } catch (SQLException e) {
            if(e.getErrorCode() == 2627) return ErrorCode.CATEGORY_NAME_ALREADY_EXISTS;

            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public ErrorCode updateCategory(Category category) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("UPDATE category SET name = ?, color = ? WHERE id = ?")){
                ErrorCode errCode = CategoryValidator.isCategoryIDValid(category.getId());
                if(errCode != ErrorCode.OK) return errCode;
                
                if(category.getName() != null){
                    String strippedName = category.getName().strip();
                    category.setName(strippedName);
                }
                
                errCode = CategoryValidator.isCategoryNameValid(category.getName());
                if(errCode != ErrorCode.OK) return errCode;

                errCode = CategoryValidator.isColorValid(category.getColor());
                if(errCode != ErrorCode.OK) return errCode;

                stm.setString(1, category.getName());
                stm.setString(2, category.getColor());
                stm.setInt(3, category.getId());

                int rowCount = stm.executeUpdate();
                return rowCount == 1 ? ErrorCode.OK : ErrorCode.CATEGORY_DOES_NOT_EXIST;

        } catch (SQLException e) {
            if(e.getErrorCode() == 2627) return ErrorCode.CATEGORY_NAME_ALREADY_EXISTS;

            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public ErrorCode deleteCategory(Integer id) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("DELETE FROM category WHERE id = ?")){
                ErrorCode errCode = CategoryValidator.isCategoryIDValid(id);
                if(errCode != ErrorCode.OK) return errCode;

                stm.setInt(1, id);

                int rowCount = stm.executeUpdate();
                return rowCount == 1 ? ErrorCode.OK : ErrorCode.CATEGORY_DOES_NOT_EXIST;

        } catch (SQLException e) {
            if(e.getErrorCode() == 547) return ErrorCode.CATEGORY_TRANSACTIONS_EXIST;
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
    }

    public record Result(List<Category> list, ErrorCode errCode) {}

    public Result getCategories(){
        try(Connection con = DB.source().getConnection();
            PreparedStatement stm = con.prepareStatement("SELECT * FROM category ORDER BY name")){            

            ResultSet rs = stm.executeQuery();
            List<Category> categories = new ArrayList<>();

            while(rs.next()){
                categories.add(new Category(rs.getInt("id"), rs.getString("name"), rs.getString("color")));
            }

            return new Result(categories, ErrorCode.OK);

        } catch (SQLException e){
            return new Result(null, ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
