package com.wavii.service;

import com.stripe.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @InjectMocks
    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "");
        ReflectionTestUtils.setField(stripeService, "pricePlus", "price_plus_test");
        ReflectionTestUtils.setField(stripeService, "priceScholar", "price_scholar_test");
        ReflectionTestUtils.setField(stripeService, "couponScholarPromo", "");
    }

    // ── isConfigured ──────────────────────────────────────────────

    @Test
    void isConfiguredWithValidKeyReturnsTrueTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_test_123abc");
        assertTrue(stripeService.isConfigured());
    }

    @Test
    void isConfiguredWithEmptyKeyReturnsFalseTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertFalse(stripeService.isConfigured());
    }

    @Test
    void isConfiguredWithNullKeyReturnsFalseTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", null);
        assertFalse(stripeService.isConfigured());
    }

    @Test
    void isConfiguredWithBlankKeyReturnsFalseTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "   ");
        assertFalse(stripeService.isConfigured());
    }

    @Test
    void isConfiguredWithSingleCharKeyReturnsTrueTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "x");
        assertTrue(stripeService.isConfigured());
    }

    // ── init ──────────────────────────────────────────────────────

    @Test
    void initWithValidKeySetsStripeApiKeyTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_test_abc123");
        stripeService.init();
        assertEquals("sk_test_abc123", com.stripe.Stripe.apiKey);
    }

    @Test
    void initWithEmptyKeyDoesNotThrowTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertDoesNotThrow(() -> stripeService.init());
    }

    @Test
    void initWithNullKeyDoesNotThrowTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", null);
        assertDoesNotThrow(() -> stripeService.init());
    }

    @Test
    void initWithBlankKeyDoesNotThrowTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "   ");
        assertDoesNotThrow(() -> stripeService.init());
    }

    @Test
    void initCalledTwiceDoesNotThrowTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_test_double");
        assertDoesNotThrow(() -> {
            stripeService.init();
            stripeService.init();
        });
        assertEquals("sk_test_double", com.stripe.Stripe.apiKey);
    }

    // ── constructWebhookEvent ─────────────────────────────────────

    @Test
    void constructWebhookEventNoWebhookSecretParsesPayloadWithGsonTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "");
        String payload = "{\"id\":\"evt_test_123\",\"type\":\"invoice.payment_succeeded\","
                + "\"object\":\"event\",\"livemode\":false}";

        Event event = stripeService.constructWebhookEvent(payload, null);

        assertNotNull(event);
    }

    @Test
    void constructWebhookEventNullWebhookSecretParsesWithGsonTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", null);
        String payload = "{\"id\":\"evt_test_456\",\"type\":\"customer.subscription.deleted\","
                + "\"object\":\"event\",\"livemode\":false}";

        Event event = stripeService.constructWebhookEvent(payload, "any_sig");

        assertNotNull(event);
    }

    @Test
    void constructWebhookEventBlankWebhookSecretParsesWithGsonTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "   ");
        String payload = "{\"id\":\"evt_test_789\",\"type\":\"invoice.payment_failed\","
                + "\"object\":\"event\"}";

        Event event = stripeService.constructWebhookEvent(payload, null);

        assertNotNull(event);
    }

    @Test
    void constructWebhookEventNoSecretSubscriptionUpdatedEventTypeTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "");
        String payload = "{\"id\":\"evt_sub_upd\",\"type\":\"customer.subscription.updated\","
                + "\"object\":\"event\",\"livemode\":false}";

        Event event = stripeService.constructWebhookEvent(payload, null);

        assertNotNull(event);
        assertEquals("customer.subscription.updated", event.getType());
    }

    @Test
    void constructWebhookEventNoSecretSubscriptionDeletedEventTypeTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "");
        String payload = "{\"id\":\"evt_sub_del\",\"type\":\"customer.subscription.deleted\","
                + "\"object\":\"event\",\"livemode\":false}";

        Event event = stripeService.constructWebhookEvent(payload, "irrelevant_sig");

        assertNotNull(event);
        assertEquals("customer.subscription.deleted", event.getType());
    }

    @Test
    void constructWebhookEventNoSecretPaymentFailedEventTypeTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "");
        String payload = "{\"id\":\"evt_pay_fail\",\"type\":\"invoice.payment_failed\","
                + "\"object\":\"event\",\"livemode\":false}";

        Event event = stripeService.constructWebhookEvent(payload, null);

        assertNotNull(event);
        assertEquals("invoice.payment_failed", event.getType());
    }

    @Test
    void constructWebhookEventWithWebhookSecretAndInvalidSignatureThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "whsec_real_secret");
        String payload = "{\"id\":\"evt_real\",\"type\":\"invoice.payment_succeeded\","
                + "\"object\":\"event\",\"livemode\":false}";

        assertThrows(com.stripe.exception.SignatureVerificationException.class,
                () -> stripeService.constructWebhookEvent(payload, "t=1,v1=invalidsig"));
    }

    @Test
    void constructWebhookEventWithWebhookSecretAndNullSignatureThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "whsec_real_secret");
        String payload = "{\"id\":\"evt_real2\",\"type\":\"invoice.payment_succeeded\","
                + "\"object\":\"event\"}";

        assertThrows(Exception.class,
                () -> stripeService.constructWebhookEvent(payload, null));
    }

    // ── isConfigured edge cases ───────────────────────────────────

    @Test
    void isConfiguredConsistentWithInitBehaviorTest() {
        // When secretKey is set, both isConfigured and init should agree
        ReflectionTestUtils.setField(stripeService, "secretKey", "sk_live_test_key");
        assertTrue(stripeService.isConfigured());
        assertDoesNotThrow(() -> stripeService.init());
        assertEquals("sk_live_test_key", com.stripe.Stripe.apiKey);
    }

    @Test
    void isConfiguredWhitespacePrefixKeyReturnsTrueTest() {
        // A key with spaces around it but has non-blank content is NOT blank
        // "  sk_test  ".isBlank() == false → isConfigured() returns true
        ReflectionTestUtils.setField(stripeService, "secretKey", "  sk_test  ");
        assertTrue(stripeService.isConfigured());
    }

    // ── constructWebhookEvent – event ID and livemode ────────────

    @Test
    void constructWebhookEventNoSecretPreservesEventIdTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "");
        String payload = "{\"id\":\"evt_id_check\",\"type\":\"invoice.payment_succeeded\","
                + "\"object\":\"event\",\"livemode\":false}";

        Event event = stripeService.constructWebhookEvent(payload, null);

        assertNotNull(event);
        assertEquals("evt_id_check", event.getId());
    }

    @Test
    void constructWebhookEventNoSecretLivemodeFieldTest() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", null);
        String payload = "{\"id\":\"evt_live\",\"type\":\"charge.succeeded\","
                + "\"object\":\"event\",\"livemode\":true}";

        Event event = stripeService.constructWebhookEvent(payload, "some_sig");

        assertNotNull(event);
        assertTrue(event.getLivemode());
    }

    // ── refundPaymentIntent – not configured (would throw calling Stripe) ─────

    @Test
    void refundPaymentIntentWhenNotConfiguredStripeThrowsTest() {
        // secretKey is empty so Stripe.apiKey is not set; the static API call should throw
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.refundPaymentIntent("pi_test_bogus"));
    }

    // ── createSetupIntent – not configured ───────────────────────

    @Test
    void createSetupIntentWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.createSetupIntent("cus_mock", "plus"));
    }

    // ── createClassPaymentIntent – not configured ─────────────────

    @Test
    void createClassPaymentIntentWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.createClassPaymentIntent(
                        "cus_mock", "teacher_1", "Teacher Name",
                        "Student Name", "guitar", "online", "Madrid", 5000L));
    }

    // ── createOrGetCustomer ───────────────────────────────────────

    @Test
    void createOrGetCustomerWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        com.wavii.model.User user = new com.wavii.model.User();
        user.setEmail("user@test.com");
        user.setName("Test User");
        user.setStripeCustomerId(null);

        assertThrows(Exception.class,
                () -> stripeService.createOrGetCustomer(user));
    }

    @Test
    void createOrGetCustomerExistingCustomerIdReturnsImmediatelyTest() throws Exception {
        // When user already has a stripeCustomerId, no Stripe call is made
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        com.wavii.model.User user = new com.wavii.model.User();
        user.setEmail("user@test.com");
        user.setName("Test User");
        user.setStripeCustomerId("cus_existing_123");

        String result = stripeService.createOrGetCustomer(user);

        assertEquals("cus_existing_123", result);
    }

    @Test
    void createOrGetCustomerBlankCustomerIdCallsStripeTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        com.wavii.model.User user = new com.wavii.model.User();
        user.setEmail("user@test.com");
        user.setName("Test User");
        user.setStripeCustomerId("  ");

        // Blank customerId → tries to create customer via Stripe → throws without real key
        assertThrows(Exception.class,
                () -> stripeService.createOrGetCustomer(user));
    }

    // ── cancelAtPeriodEnd – not configured ───────────────────────

    @Test
    void cancelAtPeriodEndWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.cancelAtPeriodEnd("sub_bogus_id"));
    }

    // ── reactivateSubscription – not configured ───────────────────

    @Test
    void reactivateSubscriptionWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.reactivateSubscription("sub_bogus_id"));
    }

    // ── retrievePaymentIntent – not configured ────────────────────

    @Test
    void retrievePaymentIntentWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.retrievePaymentIntent("pi_bogus"));
    }

    // ── createEphemeralKey – not configured ───────────────────────

    @Test
    void createEphemeralKeyWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.createEphemeralKey("cus_mock", "2023-10-16"));
    }

    // ── createSubscriptionFromSetupIntent – not configured ────────

    @Test
    void createSubscriptionFromSetupIntentWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.createSubscriptionFromSetupIntent(
                        "cus_mock", "seti_mock", "plus", false));
    }

    @Test
    void createSubscriptionFromSetupIntentDefaultTrialUsedFalseTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.createSubscriptionFromSetupIntent(
                        "cus_mock", "seti_mock", "scholar"));
    }

    // ── createSubscription – not configured ──────────────────────

    @Test
    void createSubscriptionWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.createSubscription(
                        "cus_mock", "plus", "pm_mock", false));
    }

    @Test
    void createSubscriptionDefaultOverloadWhenNotConfiguredThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.createSubscription(
                        "cus_mock", "scholar", "pm_mock"));
    }

    // ── changeSubscription – not configured ──────────────────────

    @Test
    void changeSubscriptionWhenNotConfiguredStripeThrowsTest() {
        ReflectionTestUtils.setField(stripeService, "secretKey", "");
        assertThrows(Exception.class,
                () -> stripeService.changeSubscription("sub_bogus", "plus", false));
    }
}
