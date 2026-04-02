package com.jimmy.monetization.monetizationbackend.payment;

import com.jimmy.monetization.monetizationbackend.order.Order;
import com.jimmy.monetization.monetizationbackend.order.OrderRepository;
import com.jimmy.monetization.monetizationbackend.order.OrderStatus;
import com.jimmy.monetization.monetizationbackend.payment.dto.InitiatePaymentRequest;
import com.jimmy.monetization.monetizationbackend.payment.dto.InitiatePaymentResponse;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 *
 * Covers:
 *   TC09  – Happy path: STK push is initiated and a PaymentAttempt is persisted
 *   TC10  – Null request body throws 400
 *   TC11  – Missing orderId throws 400
 *   TC12  – Missing / blank phoneNumber throws 400
 *   TC13  – Order not found for tenant throws 404
 *   TC14  – Completed order cannot be re-initiated (409)
 *   TC15  – Pending payment attempt blocks a duplicate initiation (409)
 *   TC16  – No tenant in context throws 401
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock OrderRepository orderRepo;
    @Mock PaymentAttemptRepository attemptRepo;
    @Mock MpesaGateway mpesaGateway;

    @InjectMocks PaymentService paymentService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private UUID tenantId;
    private UUID orderId;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        orderId  = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        pendingOrder = new Order();
        setField(pendingOrder, "id", orderId);
        pendingOrder.setTenantId(tenantId);
        pendingOrder.setStatus(OrderStatus.INITIATED);
        pendingOrder.setTotalMinor(10000);
        pendingOrder.setCurrency("KES");
        pendingOrder.setOrderNumber("ORD-TEST");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Reflective helper (sets private/package-private fields) ──────────────

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not set field: " + fieldName, e);
        }
    }

    /** Builds a PaymentAttempt that looks like it was just saved (has an id). */
    private PaymentAttempt savedAttempt(PaymentStatus status) {
        PaymentAttempt a = new PaymentAttempt();
        setField(a, "id", UUID.randomUUID());
        a.setOrderId(orderId);
        a.setStatus(status);
        a.setAmountMinor(10000);
        a.setCurrency("KES");
        a.setPhoneNumber("254712345678");
        a.setProviderReference(UUID.randomUUID().toString());
        return a;
    }

    /** Default valid request. */
    private InitiatePaymentRequest validRequest() {
        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setOrderId(orderId);
        req.setPhoneNumber("254712345678");
        return req;
    }

    // ── TC09 ─ Happy path ────────────────────────────────────────────────────

    /**
     * TC09 – Successful STK push initiation
     * Given a valid INITIATED order, the service should:
     *   1. Persist a PENDING PaymentAttempt
     *   2. Transition the order to PENDING_PAYMENT
     *   3. Call MpesaGateway.stkPush exactly once
     *   4. Return a response with PENDING status and the checkoutRequestId
     */
    @Test
    @DisplayName("TC09 – valid request creates PENDING attempt and calls MpesaGateway")
    void tc09_happyPath_stkPushInitiated() {
        // Arrange
        when(orderRepo.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(pendingOrder));
        when(attemptRepo.findTopByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.empty()); // no prior attempt

        PaymentAttempt flushed = savedAttempt(PaymentStatus.PENDING);
        when(attemptRepo.saveAndFlush(any(PaymentAttempt.class))).thenReturn(flushed);

        MpesaGatewayResult gatewayResult = new MpesaGatewayResult(
                "ws_CO_123", "MR_456", "0", "Success"
        );
        when(mpesaGateway.stkPush(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(gatewayResult);

        when(attemptRepo.save(any(PaymentAttempt.class))).thenReturn(flushed);

        // Act
        InitiatePaymentResponse resp = paymentService.initiate(validRequest());

        // Assert
        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(resp.getCheckoutRequestId()).isEqualTo("ws_CO_123");

        verify(mpesaGateway, times(1))
                .stkPush("254712345678", 10000, "Order ORD-TEST", "ORD-TEST");
        verify(orderRepo, times(1)).save(argThat(o ->
                o.getStatus() == OrderStatus.PENDING_PAYMENT));
    }

    // ── TC10 ─ Null request body ──────────────────────────────────────────────

    /**
     * TC10 – Null request body throws HTTP 400.
     */
    @Test
    @DisplayName("TC10 – null request body throws 400 BAD_REQUEST")
    void tc10_nullRequest_throws400() {
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(null))
                .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

        verifyNoInteractions(orderRepo, mpesaGateway);
    }

    // ── TC11 ─ Missing orderId ─────────────────────────────────────────────────

    /**
     * TC11 – Null orderId in request body throws HTTP 400.
     */
    @Test
    @DisplayName("TC11 – null orderId throws 400 BAD_REQUEST")
    void tc11_nullOrderId_throws400() {
        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setPhoneNumber("254712345678");
        // orderId intentionally left null

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(req))
                .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    // ── TC12 ─ Missing phoneNumber ────────────────────────────────────────────

    /**
     * TC12 – Blank phoneNumber throws HTTP 400.
     */
    @Test
    @DisplayName("TC12 – blank phoneNumber throws 400 BAD_REQUEST")
    void tc12_blankPhone_throws400() {
        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setOrderId(orderId);
        req.setPhoneNumber("   ");

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(req))
                .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

        verifyNoInteractions(orderRepo);
    }

    // ── TC13 ─ Order not found (cross-tenant or non-existent) ─────────────────

    /**
     * TC13 – Order does not exist for this tenant → HTTP 404.
     * This also covers the cross-tenant isolation case: another tenant's
     * orderId returns empty because the query is scoped by tenantId.
     */
    @Test
    @DisplayName("TC13 – order not found for tenant throws 404 NOT_FOUND")
    void tc13_orderNotFound_throws404() {
        when(orderRepo.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(validRequest()))
                .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(404));

        verifyNoInteractions(mpesaGateway);
    }

    // ── TC14 ─ Completed order blocked ───────────────────────────────────────

    /**
     * TC14 – An order that is already COMPLETED cannot be paid again → HTTP 409.
     * Verifies that no STK push is made for an already-settled order.
     */
    @Test
    @DisplayName("TC14 – COMPLETED order re-initiation throws 409 CONFLICT")
    void tc14_completedOrder_throws409() {
        pendingOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepo.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(pendingOrder));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(validRequest()))
                .satisfies(e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(409);
                    assertThat(e.getReason()).contains("already completed");
                });

        verifyNoInteractions(mpesaGateway);
    }

    /**
     * TC14b – ENTITLEMENT_GRANTED order is also blocked → HTTP 409.
     */
    @Test
    @DisplayName("TC14b – ENTITLEMENT_GRANTED order re-initiation throws 409 CONFLICT")
    void tc14b_entitlementGrantedOrder_throws409() {
        pendingOrder.setStatus(OrderStatus.ENTITLEMENT_GRANTED);
        when(orderRepo.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(pendingOrder));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(validRequest()))
                .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(409));

        verifyNoInteractions(mpesaGateway);
    }

    // ── TC15 ─ Duplicate / pending attempt blocked ────────────────────────────

    /**
     * TC15 – A PENDING payment attempt with no processedAt blocks a second
     * initiation for the same order → HTTP 409.
     * Prevents double-charging a player.
     */
    @Test
    @DisplayName("TC15 – existing PENDING attempt with no processedAt throws 409 CONFLICT")
    void tc15_pendingAttemptAlreadyExists_throws409() {
        when(orderRepo.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(pendingOrder));

        PaymentAttempt existingPending = savedAttempt(PaymentStatus.PENDING);
        // processedAt is null by default — simulates an in-flight STK push
        when(attemptRepo.findTopByOrderIdOrderByCreatedAtDesc(orderId))
                .thenReturn(Optional.of(existingPending));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(validRequest()))
                .satisfies(e -> {
                    assertThat(e.getStatusCode().value()).isEqualTo(409);
                    assertThat(e.getReason()).containsIgnoringCase("pending");
                });

        verify(attemptRepo, never()).saveAndFlush(any());
        verifyNoInteractions(mpesaGateway);
    }

    // ── TC16 ─ No tenant context ──────────────────────────────────────────────

    /**
     * TC16 – If TenantContext is empty (filter bypassed) the service throws 401.
     */
    @Test
    @DisplayName("TC16 – missing tenant context throws 401 UNAUTHORIZED")
    void tc16_noTenantContext_throws401() {
        TenantContext.clear();

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> paymentService.initiate(validRequest()))
                .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(401));

        verifyNoInteractions(orderRepo, mpesaGateway);
    }
}
