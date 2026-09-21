package nti.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nti.dao.CartItemDAO;
import nti.dao.OrderDAO;
import nti.dao.OrderItemDAO;
import nti.dao.ProductDAO;
import nti.exceptions.BusinessException;
import nti.models.CartItem;
import nti.models.Order;
import nti.models.OrderItem;
import nti.models.Product;
import nti.utils.DBConnection;

/**
 * Business logic for order placement and history.
 *
 * The heaviest transactional flow in the app — placeOrder atomically:
 *   - verifies and decrements stock (atomic UPDATE ... WHERE stock >= ?)
 *   - creates the order
 *   - creates the order items
 *   - clears the customer's cart
 */
public class OrderService {

    private final ProductDAO    productDAO    = new ProductDAO();
    private final CartItemDAO   cartItemDAO   = new CartItemDAO();
    private final OrderDAO      orderDAO      = new OrderDAO();
    private final OrderItemDAO  orderItemDAO  = new OrderItemDAO();

    // ================================================================
    // PLACE ORDER — the transactional checkout
    // ================================================================
    public Order placeOrder(long customerId,
                            String fullName,
                            String phone,
                            String address) throws BusinessException {

        // ---- 1) Validate shipping inputs ----
        fullName = trimToNull(fullName);
        phone    = trimToNull(phone);
        address  = trimToNull(address);

        if (fullName == null) {
            throw new BusinessException("Please enter your full name.");
        }
        if (phone == null) {
            throw new BusinessException("Please enter your phone number.");
        }
        if (address == null) {
            throw new BusinessException("Please enter your shipping address.");
        }

        // ---- 2) Load the cart (before the transaction) ----
        List<CartItem> items;
        try {
            items = cartItemDAO.findByCustomer(customerId);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException(
                "Could not load your cart. Please try again.", e);
        }

        if (items.isEmpty()) {
            throw new BusinessException("Your cart is empty.");
        }

        // ---- 3) Sort by product ID for deterministic lock order ----
        items.sort(Comparator.comparing(ci -> ci.getProduct().getId()));

        // ---- 4) Compute total ----
        double total = 0;
        for (CartItem ci : items) {
            total += ci.getSubtotal();
        }

        // ---- 5) Transaction ----
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 5a) Decrement stock atomically for each item
            for (CartItem ci : items) {
                Product p = ci.getProduct();
                int qty = ci.getQuantity();

                int rows = productDAO.decrementStock(conn, p.getId(), qty);
                if (rows == 0) {
                    throw new BusinessException(
                        "Sorry, out of stock: " + p.getName());
                }
            }

            // 5b) Create the order
            Order order = new Order();
            order.setCustomerId(customerId);
            order.setTotalAmount(total);
            order.setStatus("PENDING");

            long orderId = orderDAO.insert(order, conn);

            // 5c) Create order items with frozen prices
            for (CartItem ci : items) {
                OrderItem oi = new OrderItem();
                oi.setOrderId(orderId);
                oi.setProductId(ci.getProduct().getId());
                oi.setQuantity(ci.getQuantity());
                oi.setPrice(ci.getProduct().getPrice());   // ← price freeze
                orderItemDAO.insert(oi, conn);
            }

            // 5d) Clear the cart
            cartItemDAO.deleteByCustomer(customerId, conn);

            // 5e) Commit
            conn.commit();

            return order;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            e.printStackTrace();
            throw new BusinessException(
                "Could not place your order. Please try again.", e);

        } catch (BusinessException e) {
            // Re-throw business errors after rollback
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    // ================================================================
    // READ — order history
    // ================================================================

    public List<Order> getOrdersForCustomer(long customerId) {
        try {
            return orderDAO.findByCustomer(customerId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Order getOrderWithItems(long orderId, long customerId) {

        try {
            Order order = orderDAO.findById(orderId);

            // Ownership check
            if (order == null || order.getCustomerId() != customerId) {
                return null;
            }

            List<OrderItem> items = orderItemDAO.findByOrderId(orderId);
            order.setItems(items);

            return order;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Order getOrderById(long orderId) {
        try {
            Order order = orderDAO.findById(orderId);
            if (order == null) return null;

            List<OrderItem> items = orderItemDAO.findByOrderId(orderId);
            order.setItems(items);

            return order;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ================================================================
    // HELPER
    // ================================================================
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}