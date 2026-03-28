package io.govaryn.kernel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class KernelAuthenticationFailureHandlingFilter extends OncePerRequestFilter {

    private final KernelAuthenticationFailureEntryPoint authenticationFailureEntryPoint;

    public KernelAuthenticationFailureHandlingFilter(
        KernelAuthenticationFailureEntryPoint authenticationFailureEntryPoint
    ) {
        this.authenticationFailureEntryPoint = authenticationFailureEntryPoint;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            if (response.isCommitted()) {
                throw ex;
            }
            authenticationFailureEntryPoint.commence(request, response, ex);
        }
    }
}
