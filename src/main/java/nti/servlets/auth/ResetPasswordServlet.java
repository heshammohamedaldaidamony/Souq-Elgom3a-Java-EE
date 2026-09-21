package nti.servlets.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nti.exceptions.BusinessException;
import nti.services.AuthService;

public class ResetPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    // ================================================================
    // GET — show the reset-password form
    // ================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");

        if (token == null || token.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/forgot-password.jsp");
            return;
        }

        request.getRequestDispatcher("/reset-password.jsp").forward(request, response);
    }

    // ================================================================
    // POST — process the new password
    // ================================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String token           = request.getParameter("token");
        String newPassword     = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            authService.resetPassword(token, newPassword, confirmPassword);

            response.sendRedirect(request.getContextPath() + "/login.jsp?reset=1");

        } catch (BusinessException e) {
            // Failure → back to the form with the error
            request.setAttribute("error", e.getMessage());

            // Keep the token as it is in param so the user can retry without a new email
            request.getRequestDispatcher("/reset-password.jsp").forward(request, response);
        }
    }
}