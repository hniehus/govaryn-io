package io.govaryn.kernel.api;

import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

import java.util.Locale;

public interface KernelModule extends KernelRuntimeModule {

    default String moduleName() {
        return getClass().getSimpleName();
    }

    @Override
    default ModuleMetadata metadata() {
        String name = moduleName();
        return ModuleMetadata.minimal(
            defaultModuleId(name),
            name,
            "0.1.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName()
        );
    }

    default int order() {
        return 0;
    }

    @Override
    default void initialize(KernelContext context) {
        init(context);
    }

    default void init(KernelContext context) {
        // default no-op
    }

    @Override
    default void start() {
        // default no-op
    }

    @Override
    default void stop() {
        // default no-op
    }

    private static String defaultModuleId(String moduleName) {
        String normalized = moduleName
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+", "")
            .replaceAll("-+$", "");

        if (normalized.length() < 3) {
            normalized = (normalized + "-mod");
        }
        if (!normalized.isEmpty() && Character.isDigit(normalized.charAt(0))) {
            normalized = "m-" + normalized;
        }
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }
}
