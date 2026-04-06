package io.govaryn.kernel.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class KernelRequestSecurityContextWebConfiguration implements WebMvcConfigurer {

    private final KernelRequestTenantContextInterceptor requestTenantContextInterceptor;

    public KernelRequestSecurityContextWebConfiguration(
        KernelRequestTenantContextInterceptor requestTenantContextInterceptor
    ) {
        this.requestTenantContextInterceptor = requestTenantContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTenantContextInterceptor);
    }
}
