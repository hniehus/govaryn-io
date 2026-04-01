package io.govaryn.kernel.security.authorization.framework;

/**
 * Raised when module security registration is invalid or contradictory.
 */
public class ModuleSecurityRegistrationException extends RuntimeException {

    public ModuleSecurityRegistrationException(String message) {
        super(message);
    }

    public ModuleSecurityRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
