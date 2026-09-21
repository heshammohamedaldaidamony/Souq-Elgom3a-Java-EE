package nti.servlets.common;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nti.dao.CategoryDAO;
import nti.models.Category;
import nti.models.Product;
import nti.services.ProductService;

public class HomeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final int FEATURED_COUNT = 6;

    private final ProductService productService = new ProductService();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // ================================================================
    // GET — show the home page
    // ================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ---- 1) Load categories ----
        List<Category> categories;
        try {
            categories = categoryDAO.findAll();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            categories = java.util.Collections.emptyList();
        }

        // ---- 2) Load featured products (first N from browse) ----
        List<Product> featured;
        try {
            featured = productService
                .browse(null, null, 1, FEATURED_COUNT)
                .getProducts();
        } catch (Exception e) {
            e.printStackTrace();
            featured = java.util.Collections.emptyList();
        }

        // ---- 3) Expose to JSP ----
        request.setAttribute("categories", categories);
        request.setAttribute("featured",   featured);

        // ---- 4) Forward ----
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}