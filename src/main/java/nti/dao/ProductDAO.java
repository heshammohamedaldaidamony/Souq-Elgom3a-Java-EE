package nti.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nti.models.Product;
import nti.utils.DBConnection;

public class ProductDAO {

    // ================================================================
    // FIND PAGE — list products with optional filters + pagination
    // ================================================================
    public List<Product> findPage(String q,
                                  Long categoryId,
                                  int page,
                                  int pageSize) throws SQLException {

        StringBuilder sql = new StringBuilder(
            "SELECT id, category_id, name, description, price, stock " +
            "FROM products WHERE 1=1 "
        );

        // Collect parameter values in order for the ? placeholders
        List<Object> params = new ArrayList<>();

        // ---- Optional: search term ----
        if (q != null && !q.isEmpty()) {
            sql.append("AND name ILIKE ? ");
            params.add("%" + q + "%");   // wrap with % for partial matching
        }

        // ---- Optional: category filter ----
        if (categoryId != null) {
            sql.append("AND category_id = ? ");
            params.add(categoryId);
        }

        // ---- Order + pagination ----
        sql.append("ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        // ---- Execute ----
        List<Product> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Bind parameters in the same order we added them
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }
    // ================================================================
    // COUNT FILTERED — total rows matching the filters (for pagination)
    // ================================================================
    public int countFiltered(String q, Long categoryId) throws SQLException {

        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM products WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        // Same conditions as findPage — but no ORDER BY / LIMIT / OFFSET
        if (q != null && !q.isEmpty()) {
            sql.append("AND name ILIKE ? ");
            params.add("%" + q + "%");
        }

        if (categoryId != null) {
            sql.append("AND category_id = ? ");
            params.add(categoryId);
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }
    
    // ================================================================
    // FIND BY ID (with category name) — single product, joined
    // ================================================================
    public Product findByIdWithCategory(long id) throws SQLException {

        String sql =
            "SELECT p.id, p.category_id, p.name, p.description, " +
            "       p.price, p.stock, c.name AS category_name " +
            "FROM products p " +
            "LEFT JOIN categories c ON c.id = p.category_id " +
            "WHERE p.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowWithCategory(rs);
                }
                return null;
            }
        }
    }

    // ================================================================
    // HELPER
    // ================================================================
    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setCategoryId(rs.getLong("category_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setStock(rs.getInt("stock"));
        // Note: image is a BLOB and is intentionally not fetched here
        return p;
    }
    
    /**
     * Maps a ResultSet row to Product, including the optional category_name
     * column (from a JOIN). Falls back gracefully if the column isn't present.
     */
    private Product mapRowWithCategory(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setCategoryId(rs.getLong("category_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setStock(rs.getInt("stock"));
        // image is intentionally NOT loaded here

        // Category name — may be null if the LEFT JOIN found nothing
        p.setCategoryName(rs.getString("category_name"));

        return p;
    }
    
    // ================================================================
    // FIND IMAGE BY ID — reads only the BLOB (used by ImageServlet)
    // ================================================================
    public byte[] findImageById(long id) throws SQLException {

        String sql = "SELECT image FROM products WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("image");   // null if the column is NULL
                }
                return null;                        // product not found
            }
        }
    }
    
    // ================================================================
    // DECREMENT STOCK — atomic conditional update
    // Used by OrderService.placeOrder within the checkout transaction.
    // ================================================================
    /**
     * Atomically decrement stock, but only if there is enough.
     *
     * The `AND stock >= ?` clause is the key: without it, two concurrent
     * buyers could both pass a "stock check" and take stock negative.
     * With it, the row-level check happens during the update itself,
     * guaranteeing safety under READ COMMITTED (PostgreSQL default).
     *
     * @return number of rows affected (1 = success, 0 = insufficient stock)
     */
    public int decrementStock(Connection conn, long productId, int qty)
            throws SQLException {

        String sql = "UPDATE products " +
                     "SET stock = stock - ? " +
                     "WHERE id = ? AND stock >= ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setLong(2, productId);
            ps.setInt(3, qty);
            return ps.executeUpdate();
        }
    }
}