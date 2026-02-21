// com.jimmy.monetization.monetizationbackend.payment.PaymentAttemptTimeoutJob

package com.jimmy.monetization.monetizationbackend.payment;

import com.jimmy.monetization.monetizationbackend.order.Order;
import com.jimmy.monetization.monetizationbackend.order.OrderRepository;
import com.jimmy.monetization.monetizationbackend.order.OrderStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class PaymentAttemptTimeoutJob {

    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    private final PaymentAttemptRepository attemptRepo;
    private final OrderRepository orderRepo;

    public PaymentAttemptTimeoutJob(PaymentAttemptRepository attemptRepo, OrderRepository orderRepo) {
        this.attemptRepo = attemptRepo;
        this.orderRepo = orderRepo;
    }

    @Scheduled(fixedDelayString = "30000") // every 30s
    @Transactional
    public void expireStalePendingAttempts() {
        Instant cutoff = Instant.now().minus(TIMEOUT);

        List<PaymentAttempt> stale = attemptRepo
                .findByStatusAndProcessedAtIsNullAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);

        for (PaymentAttempt attempt : stale) {
            attempt.setStatus(PaymentStatus.FAILED);
            attempt.setProcessedAt(Instant.now());
            attempt.setResultDesc("Timed out waiting for callback");
            attemptRepo.save(attempt);

            // Put order back to retryable state ONLY if it's still waiting for payment
            Order order = orderRepo.findById(attempt.getOrderId()).orElse(null);
            if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.INITIATED);
                orderRepo.save(order);
            }
        }
    }
}