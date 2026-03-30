package com.jimmy.monetization.monetizationbackend.security;

import com.jimmy.monetization.monetizationbackend.tenant.Tenant;
import com.jimmy.monetization.monetizationbackend.tenant.TenantRepository;
import com.jimmy.monetization.monetizationbackend.tenant.TenantStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyAuthFilter.
 *
 * Covers:
 *   TC-S01 – Valid API key sets TenantContext and calls chain.doFilter (authenticated)
 *   TC-S02 – Missing Authorization header → 401, chain NOT called
 *   TC-S03 – "Bearer " prefix present but key is empty → 401
 *   TC-S04 – SHA-256 hash not found in DB → 401 (invalid key)
 *   TC-S05 – SHA-256 found but BCrypt mismatch → 401 (tampered key)
 *   TC-S06 – Filter is skipped for /api/v1/webhooks/* (shouldNotFilter)
 *   TC-S07 – Filter is skipped for /actuator/health (shouldNotFilter)
 *   TC-S08 – Filter is skipped for /api/v1/admin/tenants (shouldNotFilter)
 *   TC-S09 – TenantContext is ALWAYS cleared in the finally block, even on success
 *   TC-S10 – SecurityContext is ALWAYS cleared in the finally block
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock TenantRepository tenantRepository;
    @Mock ApiKeyHasher apiKeyHasher;
    @Mock FilterChain filterChain;

    private ApiKeyAuthFilter filter;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private static final String RAW_KEY       = "sk_live_testkey123";
    private static final String FAKE_SHA256   = "aabbcc1122334455aabbcc1122334455aabbcc1122334455aabbcc1122334455";
    private static final String FAKE_BCRYPT   = "$2a$10$fakehashedvalue";

    private UUID tenantId;
    private Tenant validTenant;

    @BeforeEach
    void setUp() {
        filter   = new ApiKeyAuthFilter(tenantRepository, apiKeyHasher);
        tenantId = UUID.randomUUID();

        validTenant = new Tenant();
        validTenant.setId(tenantId);
        validTenant.setName("Test Studio");
        validTenant.setStatus(TenantStatus.ACTIVE);
        validTenant.setApiKeyHashSha256(FAKE_SHA256);
        validTenant.setApiKeyHashBcrypt(FAKE_BCRYPT);

        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MockHttpServletRequest requestWithBearer(String key) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/orders");
        if (key != null) {
            req.addHeader("Authorization", "Bearer " + key);
        }
        return req;
    }

    // ── TC-S01 ─ Valid key ────────────────────────────────────────────────────

    /**
     * TC-S01 – Valid API key authenticates the request.
     * TenantContext must be populated and filterChain.doFilter must be called.
     */
    @Test
    @DisplayName("TC-S01 – valid API key sets TenantContext and forwards request")
    void tcS01_validKey_setsContextAndChainsFilter() throws Exception {
        // Arrange
        try (MockedStatic<Sha256> sha = mockStatic(Sha256.class)) {
            sha.when(() -> Sha256.hex(RAW_KEY)).thenReturn(FAKE_SHA256);
            when(tenantRepository.findByApiKeyHashSha256(FAKE_SHA256))
                    .thenReturn(Optional.of(validTenant));
            when(apiKeyHasher.matches(RAW_KEY, FAKE_BCRYPT)).thenReturn(true);

            MockHttpServletRequest  req  = requestWithBearer(RAW_KEY);
            MockHttpServletResponse resp = new MockHttpServletResponse();

            // Act
            filter.doFilterInternal(req, resp, filterChain);

            // Assert
            assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_OK);  // default — not overridden
            verify(filterChain, times(1)).doFilter(req, resp);

            // TenantContext is cleared in finally — verify it was set during the call
            // (we can't inspect it after, but we can verify the repo was used correctly)
            verify(tenantRepository).findByApiKeyHashSha256(FAKE_SHA256);
            verify(apiKeyHasher).matches(RAW_KEY, FAKE_BCRYPT);
        }
    }

    // ── TC-S02 ─ Missing Authorization header ────────────────────────────────

    /**
     * TC-S02 – No Authorization header → 401.
     * The filter must short-circuit and NOT call filterChain.doFilter.
     */
    @Test
    @DisplayName("TC-S02 – missing Authorization header returns 401 and blocks chain")
    void tcS02_missingAuthHeader_returns401() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/orders");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilterInternal(req, resp, filterChain);

        assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
        verifyNoInteractions(tenantRepository);
    }

    // ── TC-S03 ─ Empty key after "Bearer " ───────────────────────────────────

    /**
     * TC-S03 – "Bearer " prefix present but the key itself is blank → 401.
     */
    @Test
    @DisplayName("TC-S03 – empty key after Bearer prefix returns 401")
    void tcS03_emptyKeyAfterBearer_returns401() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/orders");
        req.addHeader("Authorization", "Bearer    "); // whitespace only
        MockHttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilterInternal(req, resp, filterChain);

        assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ── TC-S04 ─ SHA-256 not found (key doesn't belong to any tenant) ─────────

    /**
     * TC-S04 – SHA-256 hash not found → 401 (key doesn't exist in the system).
     */
    @Test
    @DisplayName("TC-S04 – unknown API key (SHA-256 not found) returns 401")
    void tcS04_sha256NotFound_returns401() throws Exception {
        try (MockedStatic<Sha256> sha = mockStatic(Sha256.class)) {
            sha.when(() -> Sha256.hex(RAW_KEY)).thenReturn(FAKE_SHA256);
            when(tenantRepository.findByApiKeyHashSha256(FAKE_SHA256))
                    .thenReturn(Optional.empty());

            MockHttpServletRequest  req  = requestWithBearer(RAW_KEY);
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilterInternal(req, resp, filterChain);

            assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(any(), any());
            verify(apiKeyHasher, never()).matches(anyString(), anyString());
        }
    }

    // ── TC-S05 ─ BCrypt mismatch (tampered / replayed key) ───────────────────

    /**
     * TC-S05 – SHA-256 lookup succeeds but BCrypt check fails → 401.
     * Protects against SHA-256 collision attacks: the raw key must also
     * match the BCrypt hash stored at registration time.
     */
    @Test
    @DisplayName("TC-S05 – BCrypt mismatch (tampered key) returns 401")
    void tcS05_bcryptMismatch_returns401() throws Exception {
        try (MockedStatic<Sha256> sha = mockStatic(Sha256.class)) {
            sha.when(() -> Sha256.hex(RAW_KEY)).thenReturn(FAKE_SHA256);
            when(tenantRepository.findByApiKeyHashSha256(FAKE_SHA256))
                    .thenReturn(Optional.of(validTenant));
            when(apiKeyHasher.matches(RAW_KEY, FAKE_BCRYPT)).thenReturn(false); // mismatch!

            MockHttpServletRequest  req  = requestWithBearer(RAW_KEY);
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilterInternal(req, resp, filterChain);

            assertThat(resp.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    // ── TC-S06 ─ shouldNotFilter: webhooks path ───────────────────────────────

    /**
     * TC-S06 – Requests to /api/v1/webhooks/* bypass this filter entirely.
     * The MPesa callback must be reachable without a tenant API key.
     */
    @Test
    @DisplayName("TC-S06 – /api/v1/webhooks/* is excluded from this filter")
    void tcS06_webhookPath_filterSkipped() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/webhooks/mpesa/callback");

        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    // ── TC-S07 ─ shouldNotFilter: actuator ───────────────────────────────────

    /**
     * TC-S07 – Actuator endpoints (health checks, metrics) bypass this filter.
     */
    @Test
    @DisplayName("TC-S07 – /actuator/* is excluded from this filter")
    void tcS07_actuatorPath_filterSkipped() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/actuator/health");

        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    // ── TC-S08 ─ shouldNotFilter: admin tenant creation ───────────────────────

    /**
     * TC-S08 – POST /api/v1/admin/tenants uses its own admin-key auth, not this filter.
     */
    @Test
    @DisplayName("TC-S08 – /api/v1/admin/tenants is excluded from this filter")
    void tcS08_adminTenantsPath_filterSkipped() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/admin/tenants");

        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    // ── TC-S09 / TC-S10 ─ Context cleanup in finally ─────────────────────────

    /**
     * TC-S09 / TC-S10 – Even after a successful authentication, both
     * TenantContext and SecurityContextHolder are cleared by the finally block.
     * This prevents context leakage between requests in thread-pool environments.
     */
    @Test
    @DisplayName("TC-S09/S10 – TenantContext and SecurityContext are cleared after filter completes")
    void tcS09_contextCleared_afterSuccessfulFilter() throws Exception {
        try (MockedStatic<Sha256> sha = mockStatic(Sha256.class)) {
            sha.when(() -> Sha256.hex(RAW_KEY)).thenReturn(FAKE_SHA256);
            when(tenantRepository.findByApiKeyHashSha256(FAKE_SHA256))
                    .thenReturn(Optional.of(validTenant));
            when(apiKeyHasher.matches(RAW_KEY, FAKE_BCRYPT)).thenReturn(true);

            // Capture TenantContext state mid-filter (inside the chain)
            UUID[] capturedTenantId = new UUID[1];
            doAnswer(inv -> {
                capturedTenantId[0] = TenantContext.getTenantId();
                return null;
            }).when(filterChain).doFilter(any(), any());

            MockHttpServletRequest  req  = requestWithBearer(RAW_KEY);
            MockHttpServletResponse resp = new MockHttpServletResponse();

            filter.doFilterInternal(req, resp, filterChain);

            // During the filter the context was set correctly
            assertThat(capturedTenantId[0]).isEqualTo(tenantId);

            // After the filter the context is cleared
            assertThat(TenantContext.getTenantId())
                    .as("TenantContext must be null after filter completes")
                    .isNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .as("SecurityContext must be null after filter completes")
                    .isNull();
        }
    }

    /**
     * TC-S10b – Context is also cleared when authentication FAILS (401 path).
     */
    @Test
    @DisplayName("TC-S10b – TenantContext and SecurityContext cleared even when auth fails")
    void tcS10b_contextCleared_afterFailedAuth() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/orders");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        // No header → 401 path

        filter.doFilterInternal(req, resp, filterChain);

        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
