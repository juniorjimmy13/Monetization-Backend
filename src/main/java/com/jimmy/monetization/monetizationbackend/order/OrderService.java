package com.jimmy.monetization.monetizationbackend.order;

import com.jimmy.monetization.monetizationbackend.catalog.Product;
import com.jimmy.monetization.monetizationbackend.catalog.ProductRepository;
import com.jimmy.monetization.monetizationbackend.order.dto.CreateOrderRequest;
import com.jimmy.monetization.monetizationbackend.order.dto.CreateOrderResponse;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import com.jimmy.monetization.monetizationbackend.user.User;
import com.jimmy.monetization.monetizationbackend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderNumberGenerator orderNumberGenerator;

    public OrderService(UserRepository userRepo, ProductRepository productRepo, OrderRepository orderRepo, OrderNumberGenerator orderNumberGenerator) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.orderNumberGenerator = orderNumberGenerator;
    }

    @Transactional
    public CreateOrderResponse create(CreateOrderRequest req) {
        if (req.getExternalUserId() == null || req.getExternalUserId().isBlank()) {
            throw new IllegalArgumentException("externalUserId is required");
        }
        if (req.getProductSku() == null || req.getProductSku().isBlank()) {
            throw new IllegalArgumentException("productSku is required");
        }

        var tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("No tenant in context");

        Product product = productRepo.findByTenantIdAndSku(tenantId, req.getProductSku())
                .orElseThrow(() -> new IllegalArgumentException("Unknown productSku"));

        // find or create user
        User user = userRepo.findByTenantIdAndExternalUserId(tenantId, req.getExternalUserId())
                .orElseGet(() -> {
                    User u = new User();
                    u.setTenantId(tenantId);
                    u.setExternalUserId(req.getExternalUserId());
                    u.setPhoneNumber(req.getPhoneNumber());
                    return userRepo.save(u);
                });

        // If user exists and phone is provided, update it (useful for MPesa)
        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isBlank()
                && (user.getPhoneNumber() == null || !req.getPhoneNumber().equals(user.getPhoneNumber()))) {
            user.setPhoneNumber(req.getPhoneNumber());
            userRepo.save(user);
        }

        Order order = new Order();
        order.setTenantId(tenantId);
        order.setUserId(user.getId());
        order.setProductId(product.getId());
        order.setOrderNumber(orderNumberGenerator.generate());
        order.setStatus(OrderStatus.INITIATED);
        order.setTotalMinor(product.getPriceMinor());
        order.setCurrency(product.getCurrency());

        Order saved = orderRepo.save(order);

        return new CreateOrderResponse(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getStatus(),
                product.getSku(),
                product.getName(),
                product.getPriceMinor(),
                product.getCurrency(),
                saved.getCreatedAt()
        );
    }
}
