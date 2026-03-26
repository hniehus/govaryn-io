package io.govaryn.kernel.module.identity;

import java.util.List;

public record ModuleIdentityCollision(
    String moduleId,
    List<ModuleIdentityReference> references
) {
    public ModuleIdentityCollision {
        references = references == null ? List.of() : List.copyOf(references);
    }
}
