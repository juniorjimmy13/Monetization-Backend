package com.jimmy.monetization.monetizationbackend.order;

import com.jimmy.monetization.monetizationbackend.catalog.Product;
import com.jimmy.monetization.monetizationbackend.catalog.ProductRepository;
import com.jimmy.monetization.monetizationbackend.order.dto.CreateOrderRequest;
import com.jimmy.monetization.monetizationbackend.order.dto.CreateOrderResponse;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import com.jimmy.monetization.monetizationbackend.user.User;
import com.jimmy.monetization.monetizationbackend.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 *
 * Covers:
 *   TC01 – Save Order to Database (happy path)
 *   TC02 – Data Persistence After Restart  (order fields are correctly mapped)
 *   TC03 – Idempotency / new user auto-created
 *   TC04 – Unknown productSku throws IllegalArgumentException
 *   TC05 – Missing externalUserId throws IllegalArgumentException
 *   TC06 – Missing productSku throws IllegalArgumentException
 *   TC07 – No tenant in context throws IllegalStateException
 *   TC08 – Existing user's phone number is updated when a new phone is provided
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock UserRepository userRepo;
    @Mock ProductRepository productRepo;
    @Mock OrderRepository orderRepo;
    @Mock OrderNumberGenerator orderNumberGenerator;

    @InjectMocks OrderService orderService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private UUID tenantId;
    private Product product;
    private User existingUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        product = new Product();
        product.setTenantId(tenantId);
        product.setSku("sword-001");
        product.setName("Iron Sword");
        product.setPriceMinor(10000);   // KES 100.00
        product.setCurrency("KES");

        existingUser = new User();
        existingUser.setTenantId(tenantId);
        existingUser.setExternalUserId("player-42");
        existingUser.setPhoneNumber("254712345678");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a saved Order so that orderRepo.save() returns something
     * with a non-null id, orderNumber, createdAt — just like the real DB would.
     */
    private Order buildSavedOrder(UUID userId) {
        Order o = new Order();
        o.setTenantId(tenantId);
        o.setUserId(userId);
        o.setProductId(product.getId());
        o.setOrderNumber("ORD-0001");
        o.setStatus(OrderStatus.INITIATED);
        o.setTotalMinor(product.getPriceMinor());
        o.setCurrency(product.getCurrency());
        // Simulate @PrePersist
        java.lang.reflect.Field idField;
        try {
            idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(o, UUID.randomUUID());
            java.lang.reflect.Field caField = Order.class.getDeclaredField("createdAt");
            caField.setAccessible(true);
            caField.set(o, java.time.Instant.now());
        } catch (Exception ignored) { }
        return o;
    }

    // ── TC01 ─ Happy path: order is created and response fields are correct ──

    /**
     * TC01 – Save Order to Database
     * Given a valid request, the service should persist an order with
     * status INITIATED and return a response populated from the saved entity.
     */
    @Test
    @DisplayName("TC01 – creates order and returns correctly mapped response")
    void tc01_createOrder_happyPath_returnsMappedResponse() {
        // Arrange
        when(productRepo.findByTenantIdAndSku(tenantId, "sword-001"))
                .thenReturn(Optional.of(product));
        when(userRepo.findByTenantIdAndExternalUserId(tenantId, "player-42"))
                .thenReturn(Optional.of(existingUser));
        when(orderNumberGenerator.generate()).thenReturn("ORD-0001");

        Order savedOrder = buildSavedOrder(existingUser.getId());
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setExternalUserId("player-42");
        req.setProductSku("sword-001");

        // Act
        CreateOrderResponse resp = orderService.create(req);

        // Assert
        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.INITIATED);
        assertThat(resp.getProductSku()).isEqualTo("sword-001");
        assertThat(resp.getCurrency()).isEqualTo("KES");
        assertThat(resp.getPriceMinor()).isEqualTo(10000);
        assertThat(resp.getOrderNumber()).isEqualTo("ORD-0001");
        assertThat(resp.getOrderId()).isNotNull();
        assertThat(resp.getCreatedAt()).isNotNull();

        verify(orderRepo, times(1)).save(any(Order.class));
    }

    // ── TC02 ─ Fields are correctly mapped onto the Order before save ────────

    /**
     * TC02 – Data Persistence: field mapping
     * Verifies that every field on the Order passed to orderRepo.save()
     * is set correctly — tenantId, userId, productId, currency, totalMinor,
     * status INITIATED — matching what would come out of the DB after a restart.
     */
    @Test
    @DisplayName("TC02 – persisted Order has correct tenantId, status, totalMinor and currency")
    void tc02_orderFields_areCorrectlyMappedBeforeSave() {
        // Arrange
        when(productRepo.findByTenantIdAndSku(tenantId, "sword-001"))
                .thenReturn(Optional.of(product));
        when(userRepo.findByTenantIdAndExternalUserId(tenantId, "player-42"))
                .thenReturn(Optional.of(existingUser));
        when(orderNumberGenerator.generate()).thenReturn("ORD-PERSIST");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        Order savedOrder = buildSavedOrder(existingUser.getId());
        when(orderRepo.save(captor.capture())).thenReturn(savedOrder);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setExternalUserId("player-42");
        req.setProductSku("sword-001");

        // Act
        orderService.create(req);

        // Assert – inspect the Order that was handed to the repository
        Order captured = captor.getValue();
        assertThat(captured.getTenantId()).isEqualTo(tenantId);
        assertThat(captured.getUserId()).isEqualTo(existingUser.getId());
        assertThat(captured.getProductId()).isEqualTo(product.getId());
        assertThat(captured.getStatus()).isEqualTo(OrderStatus.INITIATED);
        assertThat(captured.getTotalMinor()).isEqualTo(10000);
        assertThat(captured.getCurrency()).isEqualTo("KES");
        assertThat(captured.getOrderNumber()).isEqualTo("ORD-PERSIST");
    }

    // ── TC03 ─ New user is auto-created when not found ───────────────────────

    /**
     * TC03 – New user auto-created (idempotency side)
     * When the externalUserId is not in the DB, the service must create
     * and save a new User before creating the Order.
     */
    @Test
    @DisplayName("TC03 – new user is created when externalUserId is unknown")
    void tc03_newUser_isAutoCreated() {
        // Arrange
        when(productRepo.findByTenantIdAndSku(tenantId, "sword-001"))
                .thenReturn(Optional.of(product));
        // User not found → orElseGet branch fires
        when(userRepo.findByTenantIdAndExternalUserId(tenantId, "brand-new-player"))
                .thenReturn(Optional.empty());

        User newUser = new User();
        newUser.setTenantId(tenantId);
        newUser.setExternalUserId("brand-new-player");
        when(userRepo.save(any(User.class))).thenReturn(newUser);
        when(orderNumberGenerator.generate()).thenReturn("ORD-NEW");

        Order savedOrder = buildSavedOrder(newUser.getId());
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setExternalUserId("brand-new-player");
        req.setProductSku("sword-001");

        // Act
        CreateOrderResponse resp = orderService.create(req);

        // Assert
        assertThat(resp).isNotNull();
        // userRepo.save was called at least once (for the new user)
        verify(userRepo, atLeastOnce()).save(any(User.class));
    }

    // ── TC04 ─ Unknown productSku ────────────────────────────────────────────

    /**
     * TC04 – Unknown productSku throws IllegalArgumentException
     * No Order should be saved when the SKU does not exist for this tenant.
     */
    @Test
    @DisplayName("TC04 – unknown productSku throws IllegalArgumentException and does not persist")
    void tc04_unknownProductSku_throwsIllegalArgument() {
        // Arrange
        when(productRepo.findByTenantIdAndSku(tenantId, "ghost-item"))
                .thenReturn(Optional.empty());

        CreateOrderRequest req = new CreateOrderRequest();
        req.setExternalUserId("player-42");
        req.setProductSku("ghost-item");

        // Act & Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.create(req))
                .withMessageContaining("Unknown productSku");

        verify(orderRepo, never()).save(any());
    }

    // ── TC05 ─ Missing externalUserId ────────────────────────────────────────

    /**
     * TC05 – Null externalUserId is rejected before any DB call.
     */
    @Test
    @DisplayName("TC05 – null externalUserId throws IllegalArgumentException immediately")
    void tc05_nullExternalUserId_throwsIllegalArgument() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setProductSku("sword-001");
        // externalUserId intentionally left null

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.create(req))
                .withMessageContaining("externalUserId is required");

        verifyNoInteractions(productRepo, userRepo, orderRepo);
    }

    // ── TC06 ─ Missing productSku ─────────────────────────────────────────────

    /**
     * TC06 – Blank productSku is rejected before any DB call.
     */
    @Test
    @DisplayName("TC06 – blank productSku throws IllegalArgumentException immediately")
    void tc06_blankProductSku_throwsIllegalArgument() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setExternalUserId("player-42");
        req.setProductSku("   ");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> orderService.create(req))
                .withMessageContaining("productSku is required");

        verifyNoInteractions(productRepo, userRepo, orderRepo);
    }

    // ── TC07 ─ No tenant context ──────────────────────────────────────────────

    /**
     * TC07 – IllegalStateException when TenantContext is empty (e.g. filter bypassed).
     */
    @Test
    @DisplayName("TC07 – missing tenant context throws IllegalStateException")
    void tc07_noTenantContext_throwsIllegalState() {
        TenantContext.clear(); // simulate filter not running

        CreateOrderRequest req = new CreateOrderRequest();
        req.setExternalUserId("player-42");
        req.setProductSku("sword-001");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.create(req))
                .withMessageContaining("No tenant in context");
    }

    // ── TC08 ─ Existing user's phone is updated ───────────────────────────────

    /**
     * TC08 – When a different phoneNumber is supplied and the user already exists,
     * the user record is updated with the new phone before the order is created.
     */
    @Test
    @DisplayName("TC08 – existing user phone number is updated when a new one is provided")
    void tc08_existingUser_phoneNumberUpdated() {
        // Arrange – existing user has a different (old) phone
        existingUser.setPhoneNumber("254700000000");
        when(productRepo.findByTenantIdAndSku(tenantId, "sword-001"))
                .thenReturn(Optional.of(product));
        when(userRepo.findByTenantIdAndExternalUserId(tenantId, "player-42"))
                .thenReturn(Optional.of(existingUser));
        when(orderNumberGenerator.generate()).thenReturn("ORD-PHONE");

        Order savedOrder = buildSavedOrder(existingUser.getId());
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setExternalUserId("player-42");
        req.setProductSku("sword-001");
        req.setPhoneNumber("254799999999"); // new phone

        // Act
        orderService.create(req);

        // Assert – userRepo.save called to persist the updated phone
        assertThat(existingUser.getPhoneNumber()).isEqualTo("254799999999");
        verify(userRepo, atLeastOnce()).save(existingUser);
    }
}
