package com.jimmy.monetization.monetizationbackend.payment;

import com.jimmy.monetization.monetizationbackend.entitlement.EntitlementService;
import com.jimmy.monetization.monetizationbackend.order.Order;
import com.jimmy.monetization.monetizationbackend.order.OrderRepository;
import com.jimmy.monetization.monetizationbackend.order.OrderStatus;
import com.jimmy.monetization.monetizationbackend.payment.mpesa.dto.MpesaCallbackDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class MpesaCallbackService {

    private final PaymentAttemptRepository attemptRepo;
    private final OrderRepository orderRepo;
    private final EntitlementService entitlementService;

    public MpesaCallbackService(
            PaymentAttemptRepository attemptRepo,
            OrderRepository orderRepo,
            EntitlementService entitlementService
    ) {
        this.attemptRepo = attemptRepo;
        this.orderRepo = orderRepo;
        this.entitlementService = entitlementService;
    }

    @Transactional
    public void handle(MpesaCallbackDTO dto) {
        if (dto == null || dto.getBody() == null || dto.getBody().getStkCallback() == null) return;

        var cb = dto.getBody().getStkCallback();
        String checkoutId = cb.getCheckoutRequestID();
        if (checkoutId == null || checkoutId.isBlank()) return;

        PaymentAttempt attempt = attemptRepo.findByCheckoutRequestId(checkoutId).orElse(null);
        if (attempt == null) return;

        // ✅ Idempotency: if already processed, do nothing
        if (attempt.getProcessedAt() != null) {
            // Idempotent replay: if payment already confirmed, ensure entitlement exists
            if (attempt.getStatus() == PaymentStatus.CONFIRMED) {
                entitlementService.ensureGrantedForOrder(attempt.getOrderId());
            }
            return;
        }

        attempt.setResultCode(cb.getResultCode());
        attempt.setResultDesc(cb.getResultDesc());
        attempt.setProcessedAt(Instant.now());

        if (cb.getResultCode() == 0) {
            // Success
            String receipt = extractString(cb, "MpesaReceiptNumber");
            attempt.setMpesaReceiptNumber(receipt);
            attempt.setStatus(PaymentStatus.CONFIRMED);

            Order order = orderRepo.findById(attempt.getOrderId()).orElseThrow();
            order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
            orderRepo.save(order);

            // Grant entitlement + complete order (your EntitlementService should handle status transitions)
            entitlementService.grantForOrder(order.getId());

        } else if (cb.getResultCode() == 1032) {
            // User cancelled
            attempt.setStatus(PaymentStatus.CANCELLED);

            // Choice A: allow retry on same order
            Order order = orderRepo.findById(attempt.getOrderId()).orElseThrow();
            order.setStatus(OrderStatus.INITIATED);
            orderRepo.save(order);

        } else {
            // Other failures e.g. insufficient funds
            attempt.setStatus(PaymentStatus.FAILED);

            // Choice A: allow retry on same order
            Order order = orderRepo.findById(attempt.getOrderId()).orElseThrow();
            order.setStatus(OrderStatus.INITIATED);
            orderRepo.save(order);
        }

        attemptRepo.save(attempt);
    }

    private String extractString(MpesaCallbackDTO.StkCallback cb, String key) {
        if (cb.getCallbackMetadata() == null || cb.getCallbackMetadata().getItem() == null) return null;
        return cb.getCallbackMetadata().getItem().stream()
                .filter(i -> key.equals(i.getName()) && i.getValue() != null)
                .findFirst()
                .map(i -> String.valueOf(i.getValue()))
                .orElse(null);
    }
}
