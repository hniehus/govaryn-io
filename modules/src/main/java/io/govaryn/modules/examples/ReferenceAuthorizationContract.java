package io.govaryn.modules.examples;

/**
 * Contract constants for the reference module authorization integration fixture.
 */
public final class ReferenceAuthorizationContract {

    public static final String MODULE_ID = "reference-authz-module";
    public static final String RESOURCE_TYPE = "reference-document";

    public static final String SCOPE_READ = "reference.documents.read";
    public static final String SCOPE_WRITE = "reference.documents.write";

    private ReferenceAuthorizationContract() {
    }
}
