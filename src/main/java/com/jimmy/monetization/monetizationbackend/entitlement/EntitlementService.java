package com.jimmy.monetization.monetizationbackend.entitlement;

import com.jimmy.monetization.monetizationbackend.order.Order;
import com.jimmy.monetization.monetizationbackend.order.OrderRepository;
import com.jimmy.monetization.monetizationbackend.order.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class EntitlementService {

    private final EntitlementRepository entitlementRepo;
    private final OrderRepository orderRepo;

    public EntitlementService(EntitlementRepository entitlementRepo, OrderRepository orderRepo) {
        this.entitlementRepo = entitlementRepo;
        this.orderRepo = orderRepo;
    }

    @Transactional
    public void grantForOrder(UUID orderId) {
        System.out.println("GRANTING ENTITLEMENT for orderId=" + orderId);

        // ✅ idempotent: if already granted, do nothing
        if (entitlementRepo.findByOrderId(orderId).isPresent()) return;

        Order order = orderRepo.findById(orderId).orElseThrow();

        if (order.getStatus() != OrderStatus.PAYMENT_CONFIRMED) {
            throw new IllegalStateException("Order is not PAYMENT_CONFIRMED");
        }

        Entitlement e = new Entitlement();
        e.setTenantId(order.getTenantId());
        e.setUserId(order.getUserId());
        e.setOrderId(order.getId());
        e.setProductId(order.getProductId());
        e.setStatus(EntitlementStatus.ACTIVE);
        e.setMetadata(Map.of("source", "mpesa", "sku", "premium-sword"));


        entitlementRepo.save(e);

        order.setStatus(OrderStatus.ENTITLEMENT_GRANTED);
        orderRepo.save(order);

        // Optional final status
        order.setStatus(OrderStatus.COMPLETED);
        orderRepo.save(order);
    }
}
