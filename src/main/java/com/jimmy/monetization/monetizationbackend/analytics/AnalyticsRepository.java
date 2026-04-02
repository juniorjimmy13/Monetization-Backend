package com.jimmy.monetization.monetizationbackend.analytics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager em;

    public Object[] getSummary(UUID tenantId) {
        return (Object[]) em.createNativeQuery("""
            SELECT 
                COUNT(*) as orders,
                SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) as paid_orders,
                COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_minor ELSE 0 END),0) as revenue
            FROM orders
            WHERE tenant_id = :tenantId
        """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getRevenueTimeseries(UUID tenantId) {
        return em.createNativeQuery("""
            SELECT 
                DATE(created_at) as date,
                COALESCE(SUM(total_minor),0) as revenue
            FROM orders
            WHERE status='COMPLETED'
            AND tenant_id = :tenantId
            GROUP BY DATE(created_at)
            ORDER BY date
        """)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getTopProducts(UUID tenantId) {
        return em.createNativeQuery("""
            SELECT\s
                       p.sku As label,
                       COUNT(*) AS sales,
                       COALESCE(SUM(o.total_minor), 0) AS revenue
                   FROM orders o
                   JOIN products p\s
                     ON o.product_id = p.id
                    AND o.tenant_id = p.tenant_id
                   WHERE o.status = 'COMPLETED'
                   AND o.tenant_id = :tenantId
                   GROUP BY p.id, p.name, p.sku
                   ORDER BY revenue DESC
                   LIMIT 5;
        """)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }

    public Object[] getPlatformSummary() {
        return (Object[]) em.createNativeQuery("""
            SELECT 
                COUNT(*) as orders,
                SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) as paid_orders,
                COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_minor ELSE 0 END),0) as revenue
            FROM orders
        """)
                .getSingleResult();
    }

    public long countTenants() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM tenants")
                .getSingleResult())
                .longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getTopTenants() {
        return em.createNativeQuery("""
        SELECT 
            t.name as tenant_name,
            COUNT(o.id) as orders,
            COALESCE(SUM(o.total_minor),0) as revenue
        FROM orders o
        JOIN tenants t ON o.tenant_id = t.id
        WHERE o.status = 'COMPLETED'
        GROUP BY t.name
        ORDER BY revenue DESC
        LIMIT 10
    """)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getGlobalTopProducts() {
        return em.createNativeQuery("""
            SELECT\s
                        p.sku AS label,
                        COUNT(*) AS sales,
                        COALESCE(SUM(o.total_minor), 0) AS revenue
                    FROM orders o
                    JOIN products p\s
                      ON o.product_id = p.id
                     AND o.tenant_id = p.tenant_id
                    WHERE o.status = 'COMPLETED'
                    AND o.tenant_id = :tenantId
                    GROUP BY p.sku
                    ORDER BY revenue DESC
                    LIMIT 10;
        """)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getPlatformTimeseries() {
        return em.createNativeQuery("""
            SELECT 
                DATE(created_at) as date,
                COALESCE(SUM(total_minor),0) as revenue
            FROM orders
            WHERE status='COMPLETED'
            GROUP BY DATE(created_at)
            ORDER BY date
        """)
                .getResultList();
    }
}