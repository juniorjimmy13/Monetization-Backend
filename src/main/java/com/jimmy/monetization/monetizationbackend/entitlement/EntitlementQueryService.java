package com.jimmy.monetization.monetizationbackend.entitlement;

import com.jimmy.monetization.monetizationbackend.entitlement.dto.EntitlementView;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import com.jimmy.monetization.monetizationbackend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EntitlementQueryService {

    private final EntitlementRepository entitlementRepo;
    private final UserRepository userRepo;

    public EntitlementQueryService(EntitlementRepository entitlementRepo, UserRepository userRepo) {
        this.entitlementRepo = entitlementRepo;
        this.userRepo = userRepo;
    }

    public List<EntitlementView> listOwnedSkus(String externalUserId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("Missing tenant context");

        var user = userRepo.findByTenantIdAndExternalUserId(tenantId, externalUserId)
                .orElse(null);

        // MVP choice: unknown user => return empty list (don’t leak info)
        if (user == null) return List.of();

        return entitlementRepo.findActiveViewsByTenantAndUser(
                tenantId,
                user.getId(),
                EntitlementStatus.ACTIVE
        );
    }
}
