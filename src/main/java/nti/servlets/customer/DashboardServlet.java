package nti.servlets.customer;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import nti.exceptions.BusinessException;
import nti.models.Cart;
import nti.models.Customer;
import nti.models.User;
import nti.services.AuthService;
import nti.services.CartService;

public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();
    private final CartService cartService = new CartService();

    // ================================================================
    // GET — show the customer dashboard
    // ================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // ---- 1) Must be logged in ----
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        // ---- 2) Must be a customer (not admin) ----
        if (!"CUSTOMER".equals(user.getRole())) {
            // Admins should not be here — send them home
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        // ---- 3) Load customer profile ----
        Customer customer = null;
        try {
            customer = authService.findCustomerByUserId(user.getId());
        } catch (BusinessException e) {
            e.printStackTrace();
            // Non-fatal — the JSP will show a fallback
        }

        // ---- 4) Load the cart ----
        Cart cart = cartService.getCart(session, user);

        // ---- 5) Expose to JSP ----
        request.setAttribute("customer", customer);
        request.setAttribute("cart",     cart);

        // ---- 6) Forward ----
        request.getRequestDispatcher("/customer/dashboard.jsp").forward(request, response);
    }
}