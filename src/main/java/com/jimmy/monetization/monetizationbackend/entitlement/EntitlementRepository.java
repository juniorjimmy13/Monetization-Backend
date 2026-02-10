package com.jimmy.monetization.monetizationbackend.entitlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {
    Optional<Entitlement> findByOrderId(UUID orderId);
    List<Entitlement> findByTenantIdAndUserId(UUID tenantId, UUID userId);
    List<Entitlement> findByTenantIdAndUserIdAndProductIdAndStatus(
            UUID tenantId, UUID userId, UUID productId, EntitlementStatus status
    );
}
