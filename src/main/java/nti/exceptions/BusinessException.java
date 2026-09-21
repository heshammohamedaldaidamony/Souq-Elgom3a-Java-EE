package nti.exceptions;

/**
 * Thrown by the Service layer when a business rule is violated:
 *   - Validation failure (empty field, bad format, too short, etc.)
 *   - Uniqueness violation (username taken, email already registered)
 *   - Business state problem (account not found, token expired, etc.)
 */
public class BusinessException extends Exception {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}