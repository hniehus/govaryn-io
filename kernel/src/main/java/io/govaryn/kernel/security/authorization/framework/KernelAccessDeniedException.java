package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.DenyReason;

/**
 * Standard kernel exception for denied authorization decisions.
 */
public class KernelAccessDeniedException extends RuntimeException {

    private final DenyReason denyReason;
    private final String moduleId;
    private final String resourceType;
    private final AuthorizationAction action;
    private final String resourceId;

    public KernelAccessDeniedException(
        DenyReason denyReason,
        String moduleId,
        String resourceType,
        AuthorizationAction action,
        String resourceId
    ) {
        this(denyReason, moduleId, resourceType, action, resourceId, null);
    }

    public KernelAccessDeniedException(
        DenyReason denyReason,
        String moduleId,
        String resourceType,
        AuthorizationAction action,
        String resourceId,
        Throwable cause
    ) {
        super("Access denied: reason=" + denyReason + ", moduleId=" + moduleId + ", resourceType=" + resourceType + ", action=" + action, cause);
        this.denyReason = denyReason;
        this.moduleId = moduleId;
        this.resourceType = resourceType;
        this.action = action;
        this.resourceId = resourceId;
    }

    public DenyReason denyReason() {
        return denyReason;
    }

    public String moduleId() {
        return moduleId;
    }

    public String resourceType() {
        return resourceType;
    }

    public AuthorizationAction action() {
        return action;
    }

    public String resourceId() {
        return resourceId;
    }
}
