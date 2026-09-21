package nti.servlets.customer;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import nti.models.Order;
import nti.models.User;
import nti.services.OrderService;

public class OrderListServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final OrderService orderService = new OrderService();

    // ================================================================
    // GET — show all orders for the logged-in customer
    // ================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // ---- 1) Auth ----
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        if (!"CUSTOMER".equals(user.getRole())) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        // ---- 2) Customer ID from session ----
        Object cidObj = session.getAttribute("customerId");
        Long customerId = (cidObj instanceof Long) ? (Long) cidObj : null;
        if (customerId == null) {
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }

        // ---- 3) Load orders ----
        List<Order> orders = orderService.getOrdersForCustomer(customerId);

        // ---- 4) Expose + forward ----
        request.setAttribute("orders", orders);
        request.getRequestDispatcher("/customer/orders.jsp").forward(request, response);
    }
}