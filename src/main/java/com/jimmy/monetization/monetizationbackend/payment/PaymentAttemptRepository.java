package com.jimmy.monetization.monetizationbackend.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    Optional<PaymentAttempt> findByCheckoutRequestId(String checkoutRequestId);
    Optional<PaymentAttempt> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);
    List<PaymentAttempt> findByStatusAndProcessedAtIsNullAndCreatedAtBefore(
            PaymentStatus status,
            Instant createdAt
    );
}
