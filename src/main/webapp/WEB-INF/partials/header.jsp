<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="nti.models.User" %>
<%!
    private String ctx;
%>
<%
    ctx = request.getContextPath();

    User sessionUser = (User) session.getAttribute("user");
    boolean isLoggedIn = (sessionUser != null);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Souq Elgom3a</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <script src="<%= ctx %>/js/main.js" defer></script>
</head>
<body>

<header class="navbar">
    <div class="container">

        <a href="<%= ctx %>/index.jsp" class="navbar-brand">
            <img src="<%= ctx %>/images/logo.svg" alt="Souq Elgom3a">
            <span>Souq Elgom3a</span>
        </a>

        <div class="navbar-actions">
            <%
                // Cart count — read from session (guests) or DB (customers)
                int headerCartCount = new nti.services.CartService()
                                        .getCartItemCount(session, sessionUser);
            %>
            <a href="<%= ctx %>/cart" class="nav-icon" title="Cart">
                🛒
                <% if (headerCartCount > 0) { %>
                    <span class="nav-badge"><%= headerCartCount %></span>
                <% } %>
            </a>

            <% if (isLoggedIn) {
                    // Get customerId from session (set at login for customers)
                    Object cidObj = session.getAttribute("customerId");
                    Long customerId = (cidObj instanceof Long) ? (Long) cidObj : null;
            %>
                <% if (customerId != null) { %>
                    <!-- Customer with avatar -->
                    <a href="<%= ctx %>/customer/dashboard.jsp" class="nav-profile" title="My Account">
                        <img src="<%= ctx %>/image?type=avatar&id=<%= customerId %>"
                             alt="Profile"
                             class="nav-avatar"
                             onerror="this.onerror=null; this.src='<%= ctx %>/images/default-avatar.svg';">
                        <span>Profile</span>
                    </a>
                <% } else { %>
                    <!-- Admin or user without customer profile — use default icon -->
                    <a href="<%= ctx %>/customer/dashboard.jsp" class="nav-profile" title="My Account">
                        <img src="<%= ctx %>/images/default-avatar.svg"
                             alt="Profile"
                             class="nav-avatar">
                        <span>Profile</span>
                    </a>
                <% } %>
                <a href="<%= ctx %>/logout" class="btn btn-secondary btn-sm">Logout</a>
            <% } else { %>
                <a href="<%= ctx %>/login.jsp" class="btn btn-secondary btn-sm">Login</a>
                <a href="<%= ctx %>/register.jsp" class="btn btn-primary btn-sm">Register</a>
            <% } %>
        </div>

    </div>
</header>

<main class="main-content">