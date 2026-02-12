package com.jimmy.monetization.monetizationbackend.entitlement;

import com.jimmy.monetization.monetizationbackend.entitlement.dto.EntitlementView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {

    Optional<Entitlement> findByOrderId(UUID orderId);

    @Query("""
        select new com.jimmy.monetization.monetizationbackend.entitlement.dto.EntitlementView(
            p.sku,
            e.status,
            e.grantedAt,
            e.expiresAt,
            e.orderId
        )
        from Entitlement e
        join Product p on p.id = e.productId
        where e.tenantId = :tenantId
          and e.userId = :userId
          and e.status = :status
        order by e.grantedAt desc
    """)
    List<EntitlementView> findActiveViewsByTenantAndUser(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("status") EntitlementStatus status
    );
}
