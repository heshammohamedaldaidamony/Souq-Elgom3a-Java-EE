package nti.services;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import nti.dao.CustomerDAO;
import nti.dao.UserDAO;
import nti.exceptions.BusinessException;
import nti.models.Customer;
import nti.models.User;
import nti.utils.DBConnection;
import nti.utils.PasswordUtil;
import java.time.LocalDateTime;
import nti.dao.PasswordResetTokenDAO;
import nti.models.PasswordResetToken;
import nti.utils.ConfigUtil;
import nti.utils.MailUtil;
import nti.utils.TokenUtil;

/**
 * Business logic for authentication-related operations.
 * Knows nothing about HTTP or Sessions.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final PasswordResetTokenDAO resetTokenDAO = new PasswordResetTokenDAO();

    // ================================================================
    // REGISTER — create user + customer in one transaction
    // ================================================================
    public void register(String name,
                         String email,
                         String username,
                         String password,
                         String confirmPassword,
                         String phone,
                         String address,
                         InputStream picStream,
                         int picSize) throws BusinessException {

        // ---- 1) Normalize inputs ----
        name     = trimToNull(name);
        email    = trimToNull(email);
        username = trimToNull(username);
        phone    = trimToNull(phone);
        address  = trimToNull(address);
        // password and confirmPassword are NOT trimmed

        // ---- 2) Validate ----
        validateRegistration(name, email, username,
                             password, confirmPassword,
                             phone, address);

        // ---- 3) Uniqueness ----
        try {
            if (userDAO.existsByUsername(username)) {
                throw new BusinessException("Username is already taken.");
            }
            if (userDAO.existsByEmail(email)) {
                throw new BusinessException("Email is already registered.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException(
                "Could not verify account details. Please try again.", e);
        }

        // ---- 4) Insert (transactional) ----
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 4a) user
            User user = new User();
            user.setUsername(username);
            user.setPassword(PasswordUtil.hash(password));
            user.setRole("CUSTOMER");

            long userId = userDAO.insert(user, conn);

            // 4b) customer (with optional profile picture)
            Customer customer = new Customer();
            customer.setUserId(userId);
            customer.setName(name);
            customer.setEmail(email);
            customer.setPhone(phone);
            customer.setAddress(address);

            customerDAO.insert(customer, picStream, picSize, conn);

            // 4c) commit
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            e.printStackTrace();
            throw new BusinessException(
                "Could not create your account. Please try again.", e);

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
    // VALIDATION — throws BusinessException on the first failure
    // ================================================================
    private void validateRegistration(String name,
                                      String email,
                                      String username,
                                      String password,
                                      String confirmPassword,
                                      String phone,
                                      String address) throws BusinessException {

        if (name == null) {
            throw new BusinessException("Full name is required.");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new BusinessException(
                "Full name must be between 2 and 100 characters.");
        }

        if (email == null) {
            throw new BusinessException("Email is required.");
        }
        if (!email.matches("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException("Please enter a valid email address.");
        }

        if (username == null) {
            throw new BusinessException("Username is required.");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new BusinessException(
                "Username must be between 3 and 50 characters.");
        }
        if (!username.matches("^[A-Za-z0-9_]+$")) {
            throw new BusinessException(
                "Username can only contain letters, numbers, and underscore.");
        }

        if (password == null || password.isEmpty()) {
            throw new BusinessException("Password is required.");
        }
        if (password.length() < 6) {
            throw new BusinessException(
                "Password must be at least 6 characters.");
        }
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            throw new BusinessException("Passwords do not match.");
        }

        if (phone != null) {
            if (!phone.matches("^\\+[0-9]{1,4}\\s[0-9\\-\\s]{5,20}$")) {
                throw new BusinessException("Phone number format is invalid.");
            }
        }

        if (address != null && address.length() > 500) {
            throw new BusinessException(
                "Address is too long (max 500 characters).");
        }
    }

    // ================================================================
    // LOGIN — authenticate by username OR email
    // ================================================================
    public User login(String identifier, String password) throws BusinessException {

        identifier = trimToNull(identifier);

        if (identifier == null) {
            throw new BusinessException("Please enter your username or email.");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException("Please enter your password.");
        }

        User user;
        try {
            user = userDAO.findByUsername(identifier);
            if (user == null) {
                user = userDAO.findByEmail(identifier);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException(
                "Could not process login. Please try again.", e);
        }

        if (user == null) {
            throw new BusinessException("Invalid username or password.");
        }

        if (!PasswordUtil.verify(password, user.getPassword())) {
            throw new BusinessException("Invalid username or password.");
        }

        return user;
    }
    // ================================================================
    // REQUEST PASSWORD RESET — generate token, store it, email the link
    // ================================================================
    public void requestPasswordReset(String email) throws BusinessException {

        // ---- 1) Normalize ----
        email = trimToNull(email);
        if (email != null) {
            email = email.toLowerCase();
        }

        
        // ---- 2) Validate ----
        if (email == null) {
            throw new BusinessException("Please enter your email address.");
        }
        if (!email.matches("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException("Please enter a valid email address.");
        }

        // ---- 3) Find the user ----
        User user;
        try {
            user = userDAO.findByEmail(email);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException(
                "Could not process your request. Please try again.", e);
        }

        // ---- 4) If not found → return silently (no info leak) ----
        if (user == null) {
            return;
        }

        // ---- 5) Generate the token ----
        String token = TokenUtil.generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        PasswordResetToken prt = new PasswordResetToken(
            user.getId(), token, expiresAt);

        // ---- 6) Store the token (before emailing) ----
        try {
            resetTokenDAO.insert(prt);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException(
                "Could not process your request. Please try again.", e);
        }

        // ---- 7) Build the reset link ----
        String link = ConfigUtil.getAppBaseUrl()
                      + "/reset-password?token=" + token;

        // ---- 8) Send the email (async — returns immediately) ----
        String subject = "Reset your Souq Elgom3a password";
        String body =
            "Hello " + user.getUsername() + ",\n\n" +
            "We received a request to reset your password.\n\n" +
            "Click the link below to choose a new password:\n\n" +
            link + "\n\n" +
            "This link expires in 1 hour. If you did not request this, you can safely ignore this email.\n\n" +
            "— Souq Elgom3a";

        MailUtil.sendAsync(email, subject, body);
    }
    // ================================================================
    // RESET PASSWORD — verify token, update password, consume token
    // ================================================================
    public void resetPassword(String token,
                              String newPassword,
                              String confirmPassword) throws BusinessException {

        // ---- 1) Validate inputs ----
        token = trimToNull(token);

        if (token == null) {
            throw new BusinessException("Reset link is invalid.");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException("Password is required.");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters.");
        }
        if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
            throw new BusinessException("Passwords do not match.");
        }

        // ---- 2) Look up the token ----
        PasswordResetToken prt;
        try {
            prt = resetTokenDAO.findByToken(token);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException(
                "Could not process your request. Please try again.", e);
        }

        if (prt == null) {
            throw new BusinessException("Reset link is invalid or has been used.");
        }

        // ---- 3) Check expiry ----
        if (prt.isExpired()) {
            // Clean up so it can't be tried again
            try { resetTokenDAO.deleteByToken(token); } catch (SQLException ignored) {}
            throw new BusinessException(
                "Reset link has expired. Please request a new one.");
        }

        // ---- 4) Hash and update ----
        String newHash = PasswordUtil.hash(newPassword);

        try {
            userDAO.updatePassword(prt.getUserId(), newHash);
            resetTokenDAO.deleteByToken(token);   // single-use
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException(
                "Could not reset your password. Please try again.", e);
        }
    }
    
    /**
     * Look up the customer profile linked to a user account.
     * Returns null if the user has no customer row (e.g., admins).
     */
    public Customer findCustomerByUserId(long userId) throws BusinessException {
        try {
            return customerDAO.findByUserId(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BusinessException("Could not load customer profile.", e);
        }
    }
    
    // ================================================================
    // Helpers
    // ================================================================
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}