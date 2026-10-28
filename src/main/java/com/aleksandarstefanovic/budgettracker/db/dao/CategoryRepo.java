package com.aleksandarstefanovic.budgettracker.db.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.aleksandarstefanovic.budgettracker.db.DB;
import com.aleksandarstefanovic.budgettracker.models.Category;

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

            return ErrorCode.CATEGORY_SQL_ERROR;
        }
    }

    public ErrorCode updateCategory(Category category) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("UPDATE category SET name = ?, color = ? WHERE id = ?")){
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
                stm.setInt(3, category.getId());

                int rowCount = stm.executeUpdate();
                return rowCount == 1 ? ErrorCode.OK : ErrorCode.CATEGORY_DOES_NOT_EXIST;

        } catch (SQLException e) {
            if(e.getErrorCode() == 2627) return ErrorCode.CATEGORY_NAME_ALREADY_EXISTS;

            return ErrorCode.CATEGORY_SQL_ERROR;
        }
    }

    public ErrorCode deleteCategory(int id) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("DELETE FROM category WHERE id = ?")){
                stm.setInt(1, id);

                int rowCount = stm.executeUpdate();
                return rowCount == 1 ? ErrorCode.OK : ErrorCode.CATEGORY_DOES_NOT_EXIST;

        } catch (SQLException e) {
            if(e.getErrorCode() == 547) return ErrorCode.CATEGORY_TRANSACTIONS_EXIST;
            return ErrorCode.CATEGORY_SQL_ERROR;
        }
    }

    public List<Category> getCategories(){
        try(Connection con = DB.source().getConnection();
            PreparedStatement stm = con.prepareStatement("SELECT * FROM category ORDER BY name")){            

            ResultSet rs = stm.executeQuery();
            List<Category> categories = new ArrayList<>();

            while(rs.next()){
                categories.add(new Category(rs.getInt("id"), rs.getString("name"), rs.getString("color")));
            }

            return categories;

        } catch (SQLException e){
            return null;
        }
    }
}
