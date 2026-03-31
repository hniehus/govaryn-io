package io.govaryn.kernel.security.authorization.framework.model;

/**
 * Deny reason taxonomy for consistent enforcement behavior.
 */
public enum DenyReason {
    SUBJECT_NOT_AUTHENTICATED,
    MODULE_ACCESS_DENIED,
    RESOURCE_ACCESS_DENIED,
    RECORD_ACCESS_DENIED,
    MODULE_SECURITY_NOT_REGISTERED,
    INVALID_REQUEST,
    EVALUATION_ERROR
}
