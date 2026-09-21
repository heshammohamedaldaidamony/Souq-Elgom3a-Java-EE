package nti.servlets.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nti.exceptions.BusinessException;
import nti.services.AuthService;

public class ForgotPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    // ================================================================
    // GET — show the forgot-password form
    // ================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
    }

    // ================================================================
    // POST — process the email, send reset link
    // ================================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");

        try {
            authService.requestPasswordReset(email);

            // Always the same message — whether the email exists or not
            request.setAttribute("success",
                "If that email is registered, we've sent a reset link. " +
                "Please check your inbox (and spam folder).");

        } catch (BusinessException e) {
            // Validation errors (empty email, bad format)
            request.setAttribute("error", e.getMessage());
        }

        request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
    }
}