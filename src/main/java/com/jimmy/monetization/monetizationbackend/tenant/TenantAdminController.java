package com.jimmy.monetization.monetizationbackend.tenant;

import com.jimmy.monetization.monetizationbackend.tenant.dto.CreateTenantRequest;
import com.jimmy.monetization.monetizationbackend.tenant.dto.CreateTenantResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @Value("${admin.api.key}")
    private String adminApiKey;

    public TenantAdminController(TenantAdminService tenantAdminService) {
        this.tenantAdminService = tenantAdminService;
    }

    @PostMapping("/tenants")
    public ResponseEntity<?> createTenant(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody CreateTenantRequest request
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing Authorization: Bearer <admin_api_key>");
        }
        String key = authorization.substring("Bearer ".length()).trim();
        if (!adminApiKey.equals(key)) {
            return ResponseEntity.status(403).body("Invalid admin API key");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("name is required");
        }

        CreateTenantResponse resp = tenantAdminService.createTenant(request);
        return ResponseEntity.status(201).body(resp);
    }
}
