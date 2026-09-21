package nti.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nti.models.CartItem;
import nti.models.Product;
import nti.utils.DBConnection;

public class CartItemDAO {

    // ================================================================
    // FIND BY CUSTOMER — full cart items with joined product details
    // ================================================================
    public List<CartItem> findByCustomer(long customerId) throws SQLException {

        String sql =
            "SELECT ci.quantity, " +
            "       p.id, p.category_id, p.name, p.description, p.price, p.stock " +
            "FROM cart_items ci " +
            "JOIN products p ON p.id = ci.product_id " +
            "WHERE ci.customer_id = ? " +
            "ORDER BY ci.added_at";

        List<CartItem> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    // ================================================================
    // INSERT — new cart item (assumes not already present)
    // ================================================================
    public void insert(long customerId, long productId, int quantity)
            throws SQLException {

        String sql = "INSERT INTO cart_items (customer_id, product_id, quantity) " +
                     "VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);

            ps.executeUpdate();
        }
    }

    // ================================================================
    // ADD OR INCREMENT — insert if not present, sum quantities if present
    // (uses PostgreSQL's INSERT ... ON CONFLICT ... DO UPDATE)
    // ================================================================
    public void addOrIncrement(long customerId, long productId, int quantity)
            throws SQLException {

        String sql =
            "INSERT INTO cart_items (customer_id, product_id, quantity) " +
            "VALUES (?, ?, ?) " +
            "ON CONFLICT (customer_id, product_id) " +
            "DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);

            ps.executeUpdate();
        }
    }

    // ================================================================
    // UPDATE QUANTITY — set a new quantity for an existing item
    // ================================================================
    public boolean updateQuantity(long customerId, long productId, int quantity)
            throws SQLException {

        String sql = "UPDATE cart_items SET quantity = ? " +
                     "WHERE customer_id = ? AND product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setLong(2, customerId);
            ps.setLong(3, productId);

            return ps.executeUpdate() > 0;
        }
    }

    // ================================================================
    // DELETE — remove one product from a customer's cart
    // ================================================================
    public boolean delete(long customerId, long productId) throws SQLException {

        String sql = "DELETE FROM cart_items " +
                     "WHERE customer_id = ? AND product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.setLong(2, productId);

            return ps.executeUpdate() > 0;
        }
    }

    // ================================================================
    // DELETE BY CUSTOMER — wipe the entire cart of one customer
    // ================================================================
    public void deleteByCustomer(long customerId) throws SQLException {

        String sql = "DELETE FROM cart_items WHERE customer_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.executeUpdate();
        }
    }
    // ================================================================
    // HELPER — map a joined row to CartItem (with embedded Product)
    // ================================================================
    private CartItem mapRow(ResultSet rs) throws SQLException {

        // Build the product from the joined columns
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setCategoryId(rs.getLong("category_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setStock(rs.getInt("stock"));
        // Note: image is not loaded here

        // Build the cart item
        CartItem item = new CartItem();
        item.setProduct(p);
        item.setQuantity(rs.getInt("quantity"));

        return item;
    }
    // ================================================================
    // DELETE BY CUSTOMER (transactional) — used inside placeOrder
    // ================================================================
    public void deleteByCustomer(long customerId, Connection conn)
            throws SQLException {

        String sql = "DELETE FROM cart_items WHERE customer_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.executeUpdate();
        }
    }
}