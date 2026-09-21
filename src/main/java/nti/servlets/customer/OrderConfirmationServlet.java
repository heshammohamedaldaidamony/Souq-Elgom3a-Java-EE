package nti.servlets.customer;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import nti.models.Order;
import nti.models.User;
import nti.services.OrderService;

public class OrderConfirmationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final OrderService orderService = new OrderService();

    // ================================================================
    // GET — show order confirmation
    // ================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // ---- 1) Must be logged in as customer ----
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

        // ---- 3) Read the order id ----
        String idStr = request.getParameter("id");
        long orderId;
        try {
            orderId = Long.parseLong(idStr);
        } catch (NumberFormatException | NullPointerException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ---- 4) Load the order (with ownership check) ----
        Order order = orderService.getOrderWithItems(orderId, customerId);

        if (order == null) {
            // Not found OR belongs to a different customer → 404
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ---- 5) Expose + forward ----
        request.setAttribute("order", order);
        request.getRequestDispatcher("/customer/order-confirmation.jsp")
               .forward(request, response);
    }
}