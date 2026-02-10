package com.jimmy.monetization.monetizationbackend.entitlement;

import com.jimmy.monetization.monetizationbackend.catalog.Product;
import com.jimmy.monetization.monetizationbackend.catalog.ProductRepository;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import com.jimmy.monetization.monetizationbackend.user.User;
import com.jimmy.monetization.monetizationbackend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EntitlementQueryService {

    private final EntitlementRepository entitlementRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    public EntitlementQueryService(
            EntitlementRepository entitlementRepo,
            UserRepository userRepo,
            ProductRepository productRepo
    ) {
        this.entitlementRepo = entitlementRepo;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
    }

    public List<Entitlement> findActiveEntitlements(String externalUserId, String productSku) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("Missing tenant context");

        User user = userRepo.findByTenantIdAndExternalUserId(tenantId, externalUserId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user"));

        Product product = productRepo.findByTenantIdAndSku(tenantId, productSku)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product"));

        return entitlementRepo.findByTenantIdAndUserIdAndProductIdAndStatus(
                tenantId, user.getId(), product.getId(), EntitlementStatus.ACTIVE
        );

    }
}
