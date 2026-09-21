package nti.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nti.models.Category;
import nti.utils.DBConnection;

public class CategoryDAO {

    // ================================================================
    // FIND ALL — returns all categories, ordered by name
    // ================================================================
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT id, name, description FROM categories ORDER BY name";

        List<Category> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }

        return list;
    }

    // ================================================================
    // HELPER
    // ================================================================
    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        return c;
    }
}