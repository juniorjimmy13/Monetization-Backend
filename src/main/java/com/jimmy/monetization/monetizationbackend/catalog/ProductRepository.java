package com.jimmy.monetization.monetizationbackend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByTenantIdAndSku(UUID tenantId, String sku);
    List<Product> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
}
