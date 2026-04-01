package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry for module resource policy registrations.
 */
@Component
public class InMemoryResourcePolicyRegistry implements ResourcePolicyRegistry {

    private final Map<ResourceKey, ResourcePolicyRegistration> registrations = new ConcurrentHashMap<>();

    @Override
    public void register(ResourcePolicyRegistration registration) {
        ResourcePolicyRegistration normalized = requireNonNull(registration, "registration");
        ResourceKey key = new ResourceKey(normalized.moduleId(), normalized.resourceType());

        ResourcePolicyRegistration previous = registrations.putIfAbsent(key, normalized);
        if (previous != null) {
            throw new ModuleSecurityRegistrationException(
                "Conflicting resource policy registration for moduleId='"
                    + normalized.moduleId()
                    + "', resourceType='"
                    + normalized.resourceType()
                    + "': already registered with actions="
                    + formatActions(previous.supportedActions())
            );
        }
    }

    @Override
    public Optional<ResourcePolicyRegistration> resolve(String moduleId, String resourceType) {
        return Optional.ofNullable(registrations.get(new ResourceKey(
            requireNonBlank(moduleId, "moduleId"),
            requireNonBlank(resourceType, "resourceType")
        )));
    }

    @Override
    public List<ResourcePolicyRegistration> findByModuleId(String moduleId) {
        String normalizedModuleId = requireNonBlank(moduleId, "moduleId");
        return registrations.entrySet().stream()
            .filter(entry -> entry.getKey().moduleId().equals(normalizedModuleId))
            .map(Map.Entry::getValue)
            .sorted(Comparator.comparing(ResourcePolicyRegistration::resourceType))
            .toList();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ModuleSecurityRegistrationException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new ModuleSecurityRegistrationException(fieldName + " must not be null");
        }
        return value;
    }

    private static String formatActions(Set<AuthorizationAction> actions) {
        return actions.stream().map(Enum::name).sorted().toList().toString();
    }

    private record ResourceKey(String moduleId, String resourceType) {
    }
}
