package com.jimmy.monetization.monetizationbackend.payment;

import com.jimmy.monetization.monetizationbackend.order.Order;
import com.jimmy.monetization.monetizationbackend.order.OrderRepository;
import com.jimmy.monetization.monetizationbackend.order.OrderStatus;
import com.jimmy.monetization.monetizationbackend.payment.dto.InitiatePaymentRequest;
import com.jimmy.monetization.monetizationbackend.payment.dto.InitiatePaymentResponse;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        if (req.getOrderId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phoneNumber is required");

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No tenant in context");

        Order order = orderRepo.findByIdAndTenantId(req.getOrderId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // If already completed, stop
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.ENTITLEMENT_GRANTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already completed");
        }

        // Retry guard: if latest attempt is still pending, block
        attemptRepo.findTopByOrderIdOrderByCreatedAtDesc(order.getId()).ifPresent(last -> {
            if (last.getStatus() == PaymentStatus.PENDING && last.getProcessedAt() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Payment already pending for this order. Wait for callback or timeout.");
            }
        });

        // Create new attempt (retry allowed)
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setOrderId(order.getId());
        attempt.setStatus(PaymentStatus.PENDING);
        attempt.setAmountMinor(order.getTotalMinor());
        attempt.setCurrency(order.getCurrency());
        attempt.setPhoneNumber(req.getPhoneNumber());
        attempt.setProviderReference(UUID.randomUUID().toString()); // NOT NULL

        attempt = attemptRepo.saveAndFlush(attempt); // ✅ ensures id exists

        // Move order into pending payment
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderRepo.save(order);

        // Call Daraja gateway
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
                "STK push request accepted for processing"
        );
    }
}