package com.jimmy.monetization.monetizationbackend.tenant;

import com.jimmy.monetization.monetizationbackend.security.ApiKeyHasher;
import com.jimmy.monetization.monetizationbackend.security.Sha256;
import com.jimmy.monetization.monetizationbackend.tenant.dto.CreateTenantRequest;
import com.jimmy.monetization.monetizationbackend.tenant.dto.CreateTenantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAdminService {

    private final TenantRepository tenantRepository;
    private final TenantKeyGenerator keyGenerator;
    private final ApiKeyHasher apiKeyHasher;

    public TenantAdminService(TenantRepository tenantRepository, TenantKeyGenerator keyGenerator, ApiKeyHasher apiKeyHasher) {
        this.tenantRepository = tenantRepository;
        this.keyGenerator = keyGenerator;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Transactional
    public CreateTenantResponse createTenant(CreateTenantRequest req) {
        String rawKey = keyGenerator.generate();

        Tenant tenant = new Tenant();
        tenant.setName(req.getName());
        tenant.setWebhookUrl(req.getWebhookUrl());
        tenant.setWebhookSecret(req.getWebhookSecret());

        tenant.setApiKeyHashSha256(Sha256.hex(rawKey));
        tenant.setApiKeyHashBcrypt(apiKeyHasher.hash(rawKey));
        tenant.setApiKeyHash(tenant.getApiKeyHashBcrypt());

        Tenant saved = tenantRepository.save(tenant);

        return new CreateTenantResponse(saved.getId(), saved.getName(), rawKey);
    }
}
