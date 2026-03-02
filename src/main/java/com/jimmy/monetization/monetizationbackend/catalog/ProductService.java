package com.jimmy.monetization.monetizationbackend.catalog;

import com.jimmy.monetization.monetizationbackend.catalog.dto.CreateProductRequest;
import com.jimmy.monetization.monetizationbackend.catalog.dto.UpdateProductRequest;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Product create(CreateProductRequest req) {
        if (req.getSku() == null || req.getSku().isBlank()) throw new IllegalArgumentException("sku required");
        if (req.getName() == null || req.getName().isBlank()) throw new IllegalArgumentException("name required");
        if (req.getPriceMinor() <= 0) throw new IllegalArgumentException("priceMinor must be > 0");

        var tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("No tenant in context");

        repo.findByTenantIdAndSku(tenantId, req.getSku()).ifPresent(p -> {
            throw new IllegalArgumentException("SKU already exists for tenant");
        });

        Product p = new Product();
        p.setTenantId(tenantId);
        p.setSku(req.getSku());
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPriceMinor(req.getPriceMinor());
        if (req.getCurrency() != null && !req.getCurrency().isBlank()) {
            p.setCurrency(req.getCurrency());
        }
        return repo.save(p);

    }
    public List<Product> listForTenant() {
        return repo.findByTenantIdOrderByCreatedAtDesc(requireTenant());
    }

    public Product getBySku(String sku) {
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("sku required");
        return repo.findByTenantIdAndSku(requireTenant(), sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    @Transactional
    public Product updateBySku(String sku, UpdateProductRequest req) {
        Product p = getBySku(sku);

        if (req.getName() != null) {
            if (req.getName().isBlank()) throw new IllegalArgumentException("name cannot be blank");
            p.setName(req.getName());
        }
        if (req.getDescription() != null) {
            p.setDescription(req.getDescription());
        }
        if (req.getPriceMinor() != 0) {
            if (req.getPriceMinor() <= 0) throw new IllegalArgumentException("priceMinor must be > 0");
            p.setPriceMinor(req.getPriceMinor());
        }
        if (req.getCurrency() != null) {
            if (req.getCurrency().isBlank()) throw new IllegalArgumentException("currency cannot be blank");
            p.setCurrency(req.getCurrency());
        }

        return repo.save(p);
    }

    @Transactional
    public void deleteBySku(String sku) {
        Product p = getBySku(sku);
        repo.delete(p);
    }

    private UUID requireTenant() {
        var tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("No tenant in context");
        return tenantId;
    }
}
