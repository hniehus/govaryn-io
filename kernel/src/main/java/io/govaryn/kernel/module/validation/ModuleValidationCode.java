package io.govaryn.kernel.module.validation;

public enum ModuleValidationCode {
    REQUIRED_FIELD_MISSING,
    INVALID_FIELD_VALUE,
    DUPLICATE_MODULE_ID,
    KERNEL_API_INCOMPATIBLE,
    CONTRACT_VERSION_UNSUPPORTED
}
