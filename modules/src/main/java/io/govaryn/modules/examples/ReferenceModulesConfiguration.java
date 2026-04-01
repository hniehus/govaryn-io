package io.govaryn.modules.examples;

import io.govaryn.kernel.api.KernelModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ReferenceModulesConfiguration {

    @Bean
    @Profile("module-example-minimal")
    KernelModule minimalReferenceModule() {
        return new MinimalReferenceModule();
    }

    @Bean
    @Profile("module-example-incompatible-api")
    KernelModule incompatibleApiVersionModule() {
        return new IncompatibleApiVersionModule();
    }

    @Bean
    @Profile("module-example-duplicate-id")
    KernelModule duplicateIdModuleA() {
        return new DuplicateIdModuleA();
    }

    @Bean
    @Profile("module-example-duplicate-id")
    KernelModule duplicateIdModuleB() {
        return new DuplicateIdModuleB();
    }

    @Bean
    @Profile("module-example-failing-init")
    KernelModule failingInitializationModule() {
        return new FailingInitializationModule();
    }

    @Bean
    @Profile("module-example-missing-required-field")
    KernelModule missingRequiredFieldModule() {
        return new MissingRequiredFieldModule();
    }

    @Bean
    @Profile("module-template")
    KernelModule moduleTemplateSupportModule() {
        return new ModuleTemplateSupportModule();
    }

    @Bean
    @Profile("module-template")
    KernelModule moduleTemplateModule() {
        return new ModuleTemplateModule();
    }

    @Bean
    @Profile({"module-reference", "module-reference-failing-init"})
    KernelModule referencePlatformSupportModule() {
        return new ReferencePlatformSupportModule();
    }

    @Bean
    @Profile("module-reference")
    KernelModule referenceFeatureModule() {
        return new ReferenceFeatureModule(false);
    }

    @Bean
    @Profile("module-reference-failing-init")
    KernelModule referenceFeatureModuleFailingInit() {
        return new ReferenceFeatureModule(true);
    }

    @Bean
    @Profile("module-reference-authz-e2e")
    ReferenceAuthorizationModule referenceAuthorizationModule() {
        return new ReferenceAuthorizationModule();
    }
}
