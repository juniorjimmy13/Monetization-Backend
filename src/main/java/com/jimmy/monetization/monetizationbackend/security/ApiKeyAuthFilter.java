package com.jimmy.monetization.monetizationbackend.security;

import com.jimmy.monetization.monetizationbackend.tenant.Tenant;
import com.jimmy.monetization.monetizationbackend.tenant.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

import java.io.IOException;
import java.util.Optional;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    private final ApiKeyHasher apiKeyHasher;

    public ApiKeyAuthFilter(TenantRepository tenantRepository, ApiKeyHasher apiKeyHasher) {
        this.tenantRepository = tenantRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Allow MPesa callback + health checks + admin endpoints to have their own auth
        return path.startsWith("/api/v1/webhooks/")
                || path.startsWith("/actuator/")
                || path.equals("/api/v1/admin/tenants")
                //|| path.equals("/api/v1/admin/products")
                //b|| path.startsWith("/api/v1/admin/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth == null || !auth.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Missing Authorization: Bearer <tenant_api_key>");
                return;
            }

            String apiKey = auth.substring("Bearer ".length()).trim();
            if (apiKey.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Empty API key");
                return;
            }

            String sha = Sha256.hex(apiKey);
            Optional<Tenant> tenantOpt = tenantRepository.findByApiKeyHashSha256(sha);

            if (tenantOpt.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid API key");
                return;
            }

            Tenant tenant = tenantOpt.get();

            String bcrypt = tenant.getApiKeyHashBcrypt();
            if (bcrypt == null || !apiKeyHasher.matches(apiKey, bcrypt)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid API key");
                return;
            }

            // Set tenant context for downstream services/repos
            TenantContext.setTenantId(tenant.getId());
            var authToken = new UsernamePasswordAuthenticationToken(
                    "tenant:" + tenant.getId(),
                    null,
                    List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);


            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
