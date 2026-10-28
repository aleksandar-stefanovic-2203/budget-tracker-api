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
                ErrorCode errCode = Utils.isCategoryNameValid(category.getName());
                if(errCode != ErrorCode.NO_ERRORS) return errCode;

                errCode = Utils.isColorValid(category.getColor());
                if(errCode != ErrorCode.NO_ERRORS) return errCode;

                stm.setString(1, category.getName());
                stm.setString(2, category.getColor());

                stm.executeUpdate();
                return ErrorCode.NO_ERRORS;

        } catch (SQLException e) {
            e.printStackTrace(); // TODO: return error code for unique constraint
            return ErrorCode.CATEGORY_SQL_ERROR;
        }
    }

    public ErrorCode updateCategory(Category category) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("UPDATE category SET name = ?, color = ? WHERE id = ?")){
                ErrorCode errCode = Utils.isCategoryNameValid(category.getName());
                if(errCode != ErrorCode.NO_ERRORS) return errCode;

                errCode = Utils.isColorValid(category.getColor());
                if(errCode != ErrorCode.NO_ERRORS) return errCode;

                stm.setString(1, category.getName());
                stm.setString(2, category.getColor());
                stm.setInt(3, category.getId());

                stm.executeUpdate();
                return ErrorCode.NO_ERRORS;

        } catch (SQLException e) {
            e.printStackTrace(); // TODO: return error code for unique constraint and non-existent id
            return ErrorCode.CATEGORY_SQL_ERROR;
        }
    }

    public ErrorCode deleteCategory(int id) {
        try(Connection conn = DB.source().getConnection();
            PreparedStatement stm = conn.prepareStatement("DELETE FROM category WHERE id = ?")){
                stm.setInt(1, id);

                stm.executeUpdate();
                return ErrorCode.NO_ERRORS;

        } catch (SQLException e) {
            e.printStackTrace(); // TODO: return error code for non-existent id and if category has existing transactions
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
            e.printStackTrace();
            return null; // TODO
        }
    }
}
