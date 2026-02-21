package com.jimmy.monetization.monetizationbackend.payment;

import com.jimmy.monetization.monetizationbackend.order.Order;
import com.jimmy.monetization.monetizationbackend.order.OrderRepository;
import com.jimmy.monetization.monetizationbackend.order.OrderStatus;
import com.jimmy.monetization.monetizationbackend.payment.dto.InitiatePaymentRequest;
import com.jimmy.monetization.monetizationbackend.payment.dto.InitiatePaymentResponse;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final OrderRepository orderRepo;
    private final PaymentAttemptRepository attemptRepo;
    private final MpesaGateway mpesaGateway;

    public PaymentService(OrderRepository orderRepo, PaymentAttemptRepository attemptRepo, MpesaGateway mpesaGateway) {
        this.orderRepo = orderRepo;
        this.attemptRepo = attemptRepo;
        this.mpesaGateway = mpesaGateway;
    }

    @Transactional
    public InitiatePaymentResponse initiate(InitiatePaymentRequest req) {
        if (req.getOrderId() == null) throw new IllegalArgumentException("orderId is required");
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank())
            throw new IllegalArgumentException("phoneNumber is required");

        var tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("No tenant in context");

        Order order = orderRepo.findByIdAndTenantId(req.getOrderId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Only allow payment initiation if order not already completed
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.ENTITLEMENT_GRANTED) {
            throw new IllegalArgumentException("Order already completed");
        }

        // Create new payment attempt (multiple allowed)
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setOrderId(order.getId());
        attempt.setStatus(PaymentStatus.PENDING);
        attempt.setAmountMinor(order.getTotalMinor());
        attempt.setCurrency(order.getCurrency());
        attempt.setPhoneNumber(req.getPhoneNumber());

// MUST be set before save (NOT NULL)
        attempt.setProviderReference(UUID.randomUUID().toString());

        attempt = attemptRepo.saveAndFlush(attempt); // now insert is valid + id generated




        // Move order into pending payment (retry stays on same order)
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderRepo.save(order);

        // Call gateway (mock for now)
        MpesaGatewayResult result = mpesaGateway.stkPush(
                req.getPhoneNumber(),
                attempt.getAmountMinor(),
                "Order " + order.getOrderNumber(),
                order.getOrderNumber()
        );

        attempt.setCheckoutRequestId(result.checkoutRequestId());
        attempt.setMerchantRequestId(result.merchantRequestId());
        attempt.setResponseCode(result.responseCode());
        attempt.setResponseDescription(result.responseDescription());
        attemptRepo.save(attempt);

        return new InitiatePaymentResponse(
                attempt.getId(),
                attempt.getStatus(),
                attempt.getCheckoutRequestId(),
                attempt.getMerchantRequestId(),
                "Payment request sent (mock)" // will become real message later
        );
    }
}
