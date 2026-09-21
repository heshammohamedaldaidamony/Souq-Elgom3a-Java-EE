package nti.servlets.customer;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nti.models.Product;
import nti.services.ProductService;

public class ProductDetailsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ProductService productService = new ProductService();

    // ================================================================
    // GET — show the details of one product
    // ================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ---- 1) Read the id ----
        String idStr = request.getParameter("id");

        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException | NullPointerException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ---- 2) Look up the product ----
        Product product = productService.findById(id);

        // ---- 3) Not found → 404 ----
        if (product == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ---- 4) Expose to JSP ----
        request.setAttribute("product", product);

        // ---- 5) Forward ----
        request.getRequestDispatcher("/product-details.jsp").forward(request, response);
    }
}