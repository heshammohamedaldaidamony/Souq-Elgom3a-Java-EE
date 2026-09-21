package nti.servlets.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1) Get existing session WITHOUT creating a new one
        HttpSession session = request.getSession(false);

        // 2) Invalidate it if it exists
        if (session != null) {
            session.invalidate();
        }

        // 3) Redirect to login page with a "logged out" flag
        response.sendRedirect(request.getContextPath() + "/login.jsp?loggedout=1");
    }
}