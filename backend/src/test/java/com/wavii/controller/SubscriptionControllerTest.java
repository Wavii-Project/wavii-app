package com.wavii.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.google.gson.Gson;
import com.wavii.model.User;
import com.wavii.model.enums.Subscription;
import com.wavii.repository.UserRepository;
import com.wavii.service.OdooService;
import com.wavii.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private StripeService stripeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OdooService odooService;

    @Mock
    private Principal principal;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setName("Test User");
        user.setSubscription(Subscription.FREE);
        lenient().when(principal.getName()).thenReturn("test@test.com");
        lenient().when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    }

    // ── createSetupIntent ─────────────────────────────────────────

    @Test
    void createSetupIntentNotConfiguredReturnsMockResponseTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.createSetupIntent(
                new SubscriptionController.SetupIntentRequest("plus"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("ek_test_dev_mock", body.get("ephemeralKey"));
        assertTrue((Boolean) body.get("devMode"));
    }

    @Test
    void createSetupIntentNotConfiguredContainsAllMockFieldsTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.createSetupIntent(
                new SubscriptionController.SetupIntentRequest("scholar"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("seti_dev_mock_secret", body.get("setupIntentClientSecret"));
        assertEquals("cus_dev_mock", body.get("customerId"));
        assertEquals("", body.get("publishableKey"));
        assertFalse((Boolean) body.get("trialUsed"));
    }

    @Test
    void createSetupIntentNotConfiguredTrialUsedTrueTest() {
        when(stripeService.isConfigured()).thenReturn(false);
        user.setTrialUsed(true);

        ResponseEntity<?> result = subscriptionController.createSetupIntent(
                new SubscriptionController.SetupIntentRequest("plus"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue((Boolean) body.get("trialUsed"));
    }

    @Test
    void createSetupIntentConfiguredSuccessTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenReturn("cus_123");
        when(stripeService.createEphemeralKey("cus_123", "2023-10-16")).thenReturn("ek_live");
        when(stripeService.createSetupIntent("cus_123", "plus"))
                .thenReturn(Map.of("clientSecret", "seti_secret", "setupIntentId", "seti_123"));

        ResponseEntity<?> result = subscriptionController.createSetupIntent(
                new SubscriptionController.SetupIntentRequest("plus"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("cus_123", body.get("customerId"));
        assertEquals("ek_live", body.get("ephemeralKey"));
        verify(userRepository).save(user);
    }

    @Test
    void createSetupIntentConfiguredSavesCustomerIdTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenReturn("cus_newid");
        when(stripeService.createEphemeralKey("cus_newid", "2023-10-16")).thenReturn("ek_key");
        when(stripeService.createSetupIntent("cus_newid", "scholar"))
                .thenReturn(Map.of("clientSecret", "secret_val", "setupIntentId", "seti_456"));

        subscriptionController.createSetupIntent(
                new SubscriptionController.SetupIntentRequest("scholar"), principal);

        assertEquals("cus_newid", user.getStripeCustomerId());
    }

    @Test
    void createSetupIntentConfiguredExceptionReturnsBadRequestTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenThrow(new RuntimeException("Stripe error"));

        ResponseEntity<?> result = subscriptionController.createSetupIntent(
                new SubscriptionController.SetupIntentRequest("plus"), principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void createSetupIntentConfiguredExceptionBodyContainsMessageTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenThrow(new RuntimeException("connect timeout"));

        ResponseEntity<?> result = subscriptionController.createSetupIntent(
                new SubscriptionController.SetupIntentRequest("plus"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("connect timeout"));
    }

    @Test
    void createSetupIntentUserNotFoundThrowsExceptionTest() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                subscriptionController.createSetupIntent(
                        new SubscriptionController.SetupIntentRequest("plus"), principal));
    }

    // ── confirmSubscription ───────────────────────────────────────

    @Test
    void confirmSubscriptionNotConfiguredScholarSetsEducationTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("scholar", "seti_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.SCHOLAR, user.getSubscription());
        assertEquals("active", user.getSubscriptionStatus());
        assertTrue(user.getStripeSubscriptionId().startsWith("dev_sub_"));
    }

    @Test
    void confirmSubscriptionNotConfiguredPlusSetsPlusTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("plus", "seti_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.PLUS, user.getSubscription());
    }

    @Test
    void confirmSubscriptionNotConfiguredPlusSetsTrialUsedTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("plus", "seti_123"), principal);

        assertTrue(user.isTrialUsed());
    }

    @Test
    void confirmSubscriptionNotConfiguredScholarDoesNotSetTrialUsedTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("scholar", "seti_123"), principal);

        assertFalse(user.isTrialUsed());
    }

    @Test
    void confirmSubscriptionNotConfiguredDevModeResponseTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("scholar", "seti_abc"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("devMode"));
        assertEquals("active", body.get("status"));
    }

    @Test
    void confirmSubscriptionConfiguredActiveSuccessTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        user.setStripeCustomerId("cus_123");
        when(stripeService.createSubscriptionFromSetupIntent("cus_123", "seti_123", "scholar", false))
                .thenReturn(Map.of("subscriptionId", "sub_123", "status", "active"));

        ResponseEntity<?> result = subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("scholar", "seti_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.SCHOLAR, user.getSubscription());
        assertEquals("active", user.getSubscriptionStatus());
    }

    @Test
    void confirmSubscriptionConfiguredTrialingSetsPlusTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        user.setStripeCustomerId("cus_123");
        when(stripeService.createSubscriptionFromSetupIntent("cus_123", "seti_123", "plus", false))
                .thenReturn(Map.of("subscriptionId", "sub_123", "status", "trialing"));

        ResponseEntity<?> result = subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("plus", "seti_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.PLUS, user.getSubscription());
    }

    @Test
    void confirmSubscriptionConfiguredPlansSetTrialUsedTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        user.setStripeCustomerId("cus_123");
        when(stripeService.createSubscriptionFromSetupIntent("cus_123", "seti_123", "plus", false))
                .thenReturn(Map.of("subscriptionId", "sub_123", "status", "active"));

        subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("plus", "seti_123"), principal);

        assertTrue(user.isTrialUsed());
    }

    @Test
    void confirmSubscriptionConfiguredExceptionReturnsBadRequestTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        user.setStripeCustomerId("cus_123");
        when(stripeService.createSubscriptionFromSetupIntent(any(), any(), any()))
                .thenThrow(new RuntimeException("Stripe error"));

        ResponseEntity<?> result = subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("plus", "seti_123"), principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void confirmSubscriptionConfiguredWithCurrentPeriodEndTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        user.setStripeCustomerId("cus_123");
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_period");
        stripeResult.put("status", "active");
        stripeResult.put("currentPeriodEnd", "2025-12-31T00:00:00");
        when(stripeService.createSubscriptionFromSetupIntent("cus_123", "seti_123", "scholar", false))
                .thenReturn(stripeResult);

        subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("scholar", "seti_123"), principal);

        assertNotNull(user.getSubscriptionCurrentPeriodEnd());
    }

    @Test
    void confirmSubscriptionConfiguredWithCancelAtPeriodEndTrueTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        user.setStripeCustomerId("cus_123");
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_cancel");
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", true);
        when(stripeService.createSubscriptionFromSetupIntent("cus_123", "seti_123", "scholar", false))
                .thenReturn(stripeResult);

        subscriptionController.confirmSubscription(
                new SubscriptionController.ConfirmSubscriptionRequest("scholar", "seti_123"), principal);

        assertTrue(user.isSubscriptionCancelAtPeriodEnd());
    }

    // ── startSubscription ─────────────────────────────────────────

    @Test
    void startSubscriptionNotConfiguredScholarSetsEducationTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("scholar", "pm_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.SCHOLAR, user.getSubscription());
    }

    @Test
    void startSubscriptionNotConfiguredPlusSetsPlusTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("plus", "pm_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.PLUS, user.getSubscription());
    }

    @Test
    void startSubscriptionNotConfiguredPlusSetsTrialUsedTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("plus", "pm_456"), principal);

        assertTrue(user.isTrialUsed());
    }

    @Test
    void startSubscriptionNotConfiguredScholarDoesNotSetTrialUsedTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("scholar", "pm_456"), principal);

        assertFalse(user.isTrialUsed());
    }

    @Test
    void startSubscriptionNotConfiguredDevModeResponseTest() {
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("scholar", "pm_x"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("devMode"));
        assertEquals("active", body.get("status"));
    }

    @Test
    void startSubscriptionConfiguredActiveSuccessTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenReturn("cus_123");
        when(stripeService.createSubscription("cus_123", "scholar", "pm_123", false))
                .thenReturn(Map.of("subscriptionId", "sub_123", "status", "active"));

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("scholar", "pm_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.SCHOLAR, user.getSubscription());
    }

    @Test
    void startSubscriptionConfiguredNonActiveStatusDoesNotSetSubscriptionTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenReturn("cus_123");
        when(stripeService.createSubscription("cus_123", "plus", "pm_123", false))
                .thenReturn(Map.of("subscriptionId", "sub_123", "status", "incomplete"));

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("plus", "pm_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.FREE, user.getSubscription());
    }

    @Test
    void startSubscriptionConfiguredSavesCustomerIdTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenReturn("cus_saved");
        when(stripeService.createSubscription("cus_saved", "scholar", "pm_123", false))
                .thenReturn(Map.of("subscriptionId", "sub_s", "status", "active"));

        subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("scholar", "pm_123"), principal);

        assertEquals("cus_saved", user.getStripeCustomerId());
        verify(userRepository, times(2)).save(user);
    }

    @Test
    void startSubscriptionConfiguredExceptionReturnsBadRequestTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenThrow(new RuntimeException("Stripe error"));

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("plus", "pm_123"), principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void startSubscriptionConfiguredExceptionBodyContainsMessageTest() throws Exception {
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenThrow(new RuntimeException("payment declined"));

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("plus", "pm_123"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("payment declined"));
    }

    @Test
    void startSubscriptionConfiguredUserIsTrialUsedPassedTest() throws Exception {
        user.setTrialUsed(true);
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(user)).thenReturn("cus_trial");
        when(stripeService.createSubscription("cus_trial", "plus", "pm_123", true))
                .thenReturn(Map.of("subscriptionId", "sub_t", "status", "active"));

        ResponseEntity<?> result = subscriptionController.startSubscription(
                new SubscriptionController.StartSubscriptionRequest("plus", "pm_123"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(stripeService).createSubscription("cus_trial", "plus", "pm_123", true);
    }

    // ── cancelSubscription ────────────────────────────────────────

    @Test
    void cancelSubscriptionFreeUserReturnsBadRequestTest() {
        user.setSubscription(Subscription.FREE);

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("No tienes"));
    }

    @Test
    void cancelSubscriptionNotConfiguredSetsCancelAtPeriodEndTest() {
        user.setSubscription(Subscription.PLUS);
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(user.isSubscriptionCancelAtPeriodEnd());
        assertEquals("cancel_at_period_end", user.getSubscriptionStatus());
    }

    @Test
    void cancelSubscriptionNotConfiguredSetsPeriodEndTest() {
        user.setSubscription(Subscription.PLUS);
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.cancelSubscription(principal);

        assertNotNull(user.getSubscriptionCurrentPeriodEnd());
    }

    @Test
    void cancelSubscriptionNotConfiguredDevModeResponseTest() {
        user.setSubscription(Subscription.PLUS);
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("cancelAtPeriodEnd"));
        assertTrue((Boolean) body.get("devMode"));
    }

    @Test
    void cancelSubscriptionNotConfiguredCallsOdooTest() {
        user.setSubscription(Subscription.PLUS);
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.cancelSubscription(principal);

        verify(odooService).createSubscriptionTask(
                eq(user.getName()), eq(user.getEmail()),
                eq("Cancelación programada de suscripción"), anyString());
    }

    @Test
    void cancelSubscriptionConfiguredNoStripeIdReturnsBadRequestTest() {
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId(null);
        when(stripeService.isConfigured()).thenReturn(true);

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue(body.get("message").toString().contains("No se encontró"));
    }

    @Test
    void cancelSubscriptionConfiguredBlankStripeIdReturnsBadRequestTest() {
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("  ");
        when(stripeService.isConfigured()).thenReturn(true);

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void cancelSubscriptionConfiguredSuccessTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_active_123");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_active_123");
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", true);
        when(stripeService.cancelAtPeriodEnd("sub_active_123")).thenReturn(stripeResult);

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(user.isSubscriptionCancelAtPeriodEnd());
    }

    @Test
    void cancelSubscriptionConfiguredSuccessWithPeriodEndTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_period_123");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_period_123");
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", true);
        stripeResult.put("currentPeriodEnd", "2025-12-31T00:00:00");
        when(stripeService.cancelAtPeriodEnd("sub_period_123")).thenReturn(stripeResult);

        subscriptionController.cancelSubscription(principal);

        assertNotNull(user.getSubscriptionCurrentPeriodEnd());
    }

    @Test
    void cancelSubscriptionConfiguredCallsOdooTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_odoo_123");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("status", "active");
        when(stripeService.cancelAtPeriodEnd("sub_odoo_123")).thenReturn(stripeResult);

        subscriptionController.cancelSubscription(principal);

        verify(odooService).createSubscriptionTask(
                eq(user.getName()), eq(user.getEmail()),
                eq("Cancelación programada de suscripción"), anyString());
    }

    @Test
    void cancelSubscriptionConfiguredExceptionReturnsBadRequestTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_err_123");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.cancelAtPeriodEnd("sub_err_123"))
                .thenThrow(new RuntimeException("Cancel failed"));

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void cancelSubscriptionEducationPlanReturnsBadRequestTest() {
        user.setSubscription(Subscription.FREE);

        ResponseEntity<?> result = subscriptionController.cancelSubscription(principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    // ── reactivateSubscription ────────────────────────────────────

    @Test
    void reactivateSubscriptionNotPendingCancellationReturnsBadRequestTest() {
        user.setSubscriptionCancelAtPeriodEnd(false);

        ResponseEntity<?> result = subscriptionController.reactivateSubscription(principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue(body.get("message").toString().contains("no está pendiente"));
    }

    @Test
    void reactivateSubscriptionNotConfiguredSetsCancelAtPeriodEndFalseTest() {
        user.setSubscriptionCancelAtPeriodEnd(true);
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.reactivateSubscription(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(user.isSubscriptionCancelAtPeriodEnd());
        assertEquals("active", user.getSubscriptionStatus());
    }

    @Test
    void reactivateSubscriptionNotConfiguredCallsOdooTest() {
        user.setSubscriptionCancelAtPeriodEnd(true);
        user.setSubscription(Subscription.PLUS);
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.reactivateSubscription(principal);

        verify(odooService).createSubscriptionTask(
                eq(user.getName()), eq(user.getEmail()),
                eq("Reactivación de suscripción"), anyString());
    }

    @Test
    void reactivateSubscriptionNotConfiguredDevModeResponseTest() {
        user.setSubscriptionCancelAtPeriodEnd(true);
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.reactivateSubscription(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("cancelAtPeriodEnd"));
        assertTrue((Boolean) body.get("devMode"));
    }

    @Test
    void reactivateSubscriptionConfiguredSuccessTest() throws Exception {
        user.setSubscriptionCancelAtPeriodEnd(true);
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_react_123");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_react_123");
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", false);
        when(stripeService.reactivateSubscription("sub_react_123")).thenReturn(stripeResult);

        ResponseEntity<?> result = subscriptionController.reactivateSubscription(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(user.isSubscriptionCancelAtPeriodEnd());
        assertEquals("active", user.getSubscriptionStatus());
    }

    @Test
    void reactivateSubscriptionConfiguredWithPeriodEndTest() throws Exception {
        user.setSubscriptionCancelAtPeriodEnd(true);
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_react_period");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", false);
        stripeResult.put("currentPeriodEnd", "2026-01-31T00:00:00");
        when(stripeService.reactivateSubscription("sub_react_period")).thenReturn(stripeResult);

        subscriptionController.reactivateSubscription(principal);

        assertNotNull(user.getSubscriptionCurrentPeriodEnd());
    }

    @Test
    void reactivateSubscriptionConfiguredCallsOdooTest() throws Exception {
        user.setSubscriptionCancelAtPeriodEnd(true);
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_odoo_react");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("status", "active");
        when(stripeService.reactivateSubscription("sub_odoo_react")).thenReturn(stripeResult);

        subscriptionController.reactivateSubscription(principal);

        verify(odooService).createSubscriptionTask(
                eq(user.getName()), eq(user.getEmail()),
                eq("Reactivación de suscripción"), anyString());
    }

    @Test
    void reactivateSubscriptionConfiguredExceptionReturnsBadRequestTest() throws Exception {
        user.setSubscriptionCancelAtPeriodEnd(true);
        user.setStripeSubscriptionId("sub_err_react");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.reactivateSubscription("sub_err_react"))
                .thenThrow(new RuntimeException("Reactivate failed"));

        ResponseEntity<?> result = subscriptionController.reactivateSubscription(principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    // ── changeSubscription ────────────────────────────────────────

    @Test
    void changeSubscriptionNoStripeSubscriptionIdReturnsPaymentRequiredTest() {
        user.setStripeSubscriptionId(null);

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("plus"), principal);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue((Boolean) body.get("needsPaymentSheet"));
    }

    @Test
    void changeSubscriptionBlankStripeSubscriptionIdReturnsPaymentRequiredTest() {
        user.setStripeSubscriptionId("  ");

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, result.getStatusCode());
    }

    @Test
    void changeSubscriptionPaymentRequiredBodyContainsTrialUsedTest() {
        user.setStripeSubscriptionId(null);
        user.setTrialUsed(true);

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("plus"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue((Boolean) body.get("trialUsed"));
    }

    @Test
    void changeSubscriptionNotConfiguredSetsEducationPlanTest() {
        user.setStripeSubscriptionId("sub_change_123");
        user.setSubscription(Subscription.PLUS);
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.SCHOLAR, user.getSubscription());
        assertEquals("active", user.getSubscriptionStatus());
        assertFalse(user.isSubscriptionCancelAtPeriodEnd());
    }

    @Test
    void changeSubscriptionNotConfiguredFromPlusToScholarAppliesPromoTest() {
        user.setStripeSubscriptionId("sub_promo_mock");
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue((Boolean) body.get("promoApplied"));
        assertTrue((Boolean) body.get("devMode"));
    }

    @Test
    void changeSubscriptionNotConfiguredFromPlusToScholarTrialingNoPromoTest() {
        user.setStripeSubscriptionId("sub_trial_mock");
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("trialing");
        when(stripeService.isConfigured()).thenReturn(false);

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertFalse((Boolean) body.get("promoApplied"));
    }

    @Test
    void changeSubscriptionNotConfiguredCallsOdooTest() {
        user.setStripeSubscriptionId("sub_change_odoo");
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(false);

        subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        verify(odooService).createSubscriptionTask(
                eq(user.getName()), eq(user.getEmail()), anyString(), anyString());
    }

    @Test
    void changeSubscriptionConfiguredSuccessTest() throws Exception {
        user.setStripeSubscriptionId("sub_cfg_123");
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_cfg_123");
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", false);
        stripeResult.put("currentPeriodEnd", "2025-12-31T00:00:00");
        when(stripeService.changeSubscription(eq("sub_cfg_123"), eq("scholar"), anyBoolean()))
                .thenReturn(stripeResult);

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.SCHOLAR, user.getSubscription());
    }

    @Test
    void changeSubscriptionConfiguredCallsOdooTest() throws Exception {
        user.setStripeSubscriptionId("sub_odoo_cfg");
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_odoo_cfg");
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", false);
        stripeResult.put("currentPeriodEnd", "2025-12-31T00:00:00");
        when(stripeService.changeSubscription(eq("sub_odoo_cfg"), eq("scholar"), anyBoolean()))
                .thenReturn(stripeResult);

        subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        verify(odooService).createSubscriptionTask(
                eq(user.getName()), eq(user.getEmail()), anyString(), anyString());
    }

    @Test
    void changeSubscriptionConfiguredExceptionReturnsBadRequestTest() throws Exception {
        user.setStripeSubscriptionId("sub_cfg_err");
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.changeSubscription(any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("Change failed"));

        ResponseEntity<?> result = subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void changeSubscriptionConfiguredNullPeriodEndHandledGracefullyTest() throws Exception {
        user.setStripeSubscriptionId("sub_null_period");
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(true);
        Map<String, Object> stripeResult = new HashMap<>();
        stripeResult.put("subscriptionId", "sub_null_period");
        stripeResult.put("status", "active");
        stripeResult.put("cancelAtPeriodEnd", false);
        stripeResult.put("currentPeriodEnd", null);
        when(stripeService.changeSubscription(any(), any(), anyBoolean()))
                .thenReturn(stripeResult);

        assertDoesNotThrow(() -> subscriptionController.changeSubscription(
                new SubscriptionController.ChangeSubscriptionRequest("scholar"), principal));
    }

    // ── getStatus ─────────────────────────────────────────────────

    @Test
    void getStatusReturnsSubscriptionInfoTest() {
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionStatus("active");
        user.setStripeSubscriptionId("sub_123");

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("plus", body.get("subscription"));
        assertEquals("active", body.get("subscriptionStatus"));
        assertEquals("sub_123", body.get("stripeSubscriptionId"));
    }

    @Test
    void getStatusNullFieldsReturnsDefaultsTest() {
        user.setSubscription(Subscription.FREE);
        user.setSubscriptionStatus(null);
        user.setStripeSubscriptionId(null);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("none", body.get("subscriptionStatus"));
        assertEquals("", body.get("stripeSubscriptionId"));
    }

    @Test
    void getStatusCancelAtPeriodEndTrueTest() {
        user.setSubscription(Subscription.PLUS);
        user.setSubscriptionCancelAtPeriodEnd(true);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue((Boolean) body.get("cancelAtPeriodEnd"));
    }

    @Test
    void getStatusCancelAtPeriodEndFalseTest() {
        user.setSubscription(Subscription.FREE);
        user.setSubscriptionCancelAtPeriodEnd(false);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertFalse((Boolean) body.get("cancelAtPeriodEnd"));
    }

    @Test
    void getStatusCurrentPeriodEndNotNullTest() {
        user.setSubscription(Subscription.PLUS);
        LocalDateTime periodEnd = LocalDateTime.of(2025, 12, 31, 0, 0, 0);
        user.setSubscriptionCurrentPeriodEnd(periodEnd);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals(periodEnd.toString(), body.get("currentPeriodEnd"));
    }

    @Test
    void getStatusCurrentPeriodEndNullReturnsEmptyStringTest() {
        user.setSubscription(Subscription.FREE);
        user.setSubscriptionCurrentPeriodEnd(null);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("", body.get("currentPeriodEnd"));
    }

    @Test
    void getStatusDeletionScheduledAtNullReturnsEmptyStringTest() {
        user.setSubscription(Subscription.FREE);
        user.setDeletionScheduledAt(null);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("", body.get("deletionScheduledAt"));
    }

    @Test
    void getStatusDeletionScheduledAtNotNullTest() {
        user.setSubscription(Subscription.FREE);
        LocalDateTime deletion = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        user.setDeletionScheduledAt(deletion);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals(deletion.toString(), body.get("deletionScheduledAt"));
    }

    @Test
    void getStatusTrialUsedTrueTest() {
        user.setTrialUsed(true);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertTrue((Boolean) body.get("trialUsed"));
    }

    @Test
    void getStatusTrialUsedFalseTest() {
        user.setTrialUsed(false);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertFalse((Boolean) body.get("trialUsed"));
    }

    @Test
    void getStatusEducationSubscriptionReturnsLowercaseTest() {
        user.setSubscription(Subscription.SCHOLAR);

        ResponseEntity<?> result = subscriptionController.getStatus(principal);

        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("education", body.get("subscription"));
    }

    @Test
    void getStatusUserNotFoundThrowsTest() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> subscriptionController.getStatus(principal));
    }

    // ── handleWebhook ─────────────────────────────────────────────

    @Test
    void handleWebhookSignatureErrorReturnsBadRequestTest() throws Exception {
        when(stripeService.constructWebhookEvent(any(), any()))
                .thenThrow(new SignatureVerificationException("Invalid signature", "sig_header"));

        ResponseEntity<String> result = subscriptionController.handleWebhook("{}", "bad_sig");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Invalid signature", result.getBody());
    }

    @Test
    void handleWebhookInvoicePaymentSucceededProcessesPaymentTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\",\"amount_paid\":1000,"
                + "\"lines\":{\"data\":[{\"description\":\"plus\"}]}}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_1\",\"type\":\"invoice.payment_succeeded\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("OK", result.getBody());
        assertEquals("active", user.getSubscriptionStatus());
        verify(userRepository).save(user);
        verify(odooService).processStripePayment(eq(user.getEmail()), eq(user.getName()), eq("plus"), eq(10.0));
    }

    @Test
    void handleWebhookInvoicePaymentSucceededUserNotFoundLogsWarningTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_unknown\",\"amount_paid\":1000,"
                + "\"lines\":{\"data\":[]}}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_1\",\"type\":\"invoice.payment_succeeded\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_unknown")).thenReturn(Optional.empty());

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verifyNoInteractions(odooService);
    }

    @Test
    void handleWebhookInvoicePaymentSucceededSetsTrialUsedForPlusUserTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        user.setTrialUsed(false);
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\",\"amount_paid\":1000,"
                + "\"lines\":{\"data\":[{\"description\":\"Plus plan\"}]}}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_trial\",\"type\":\"invoice.payment_succeeded\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        subscriptionController.handleWebhook(payload, "sig");

        assertTrue(user.isTrialUsed());
    }

    @Test
    void handleWebhookInvoicePaymentSucceededNullAmountPaidDefaultsZeroTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\","
                + "\"lines\":{\"data\":[]}}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_null_amount\",\"type\":\"invoice.payment_succeeded\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(odooService).processStripePayment(eq(user.getEmail()), eq(user.getName()), anyString(), eq(0.0));
    }

    @Test
    void handleWebhookInvoicePaymentSucceededNonBlankDescriptionUsedAsPlanTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\",\"amount_paid\":500,"
                + "\"lines\":{\"data\":[{\"description\":\"scholar\"}]}}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_scholar\",\"type\":\"invoice.payment_succeeded\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        subscriptionController.handleWebhook(payload, "sig");

        verify(odooService).processStripePayment(eq(user.getEmail()), eq(user.getName()), eq("scholar"), eq(5.0));
    }

    @Test
    void handleWebhookSubscriptionDeletedSetsUserFreeTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"canceled\"}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_2\",\"type\":\"customer.subscription.deleted\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.FREE, user.getSubscription());
        assertEquals("canceled", user.getSubscriptionStatus());
    }

    @Test
    void handleWebhookSubscriptionDeletedUnpaidSetsUserFreeTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"unpaid\"}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_2\",\"type\":\"customer.subscription.deleted\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.FREE, user.getSubscription());
    }

    @Test
    void handleWebhookSubscriptionDeletedClearsStripeSubscriptionIdTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        user.setStripeSubscriptionId("sub_to_clear");
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"canceled\"}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_del_clear\",\"type\":\"customer.subscription.deleted\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        subscriptionController.handleWebhook(payload, "sig");

        assertNull(user.getStripeSubscriptionId());
        assertFalse(user.isSubscriptionCancelAtPeriodEnd());
    }

    @Test
    void handleWebhookSubscriptionUpdatedSetsStatusAndCancelAtEndTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\",\"status\":\"active\","
                + "\"cancel_at_period_end\":true,\"current_period_end\":1800000000}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_upd\",\"type\":\"customer.subscription.updated\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("active", user.getSubscriptionStatus());
        assertTrue(user.isSubscriptionCancelAtPeriodEnd());
        assertNotNull(user.getSubscriptionCurrentPeriodEnd());
    }

    @Test
    void handleWebhookSubscriptionUpdatedNullStatusDoesNotOverwriteTest() throws Exception {
        user.setSubscriptionStatus("trialing");
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\","
                + "\"cancel_at_period_end\":false}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_upd_null\",\"type\":\"customer.subscription.updated\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        subscriptionController.handleWebhook(payload, "sig");

        assertEquals("trialing", user.getSubscriptionStatus());
    }

    @Test
    void handleWebhookSubscriptionUpdatedUserNotFoundNoInteractionTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_nobody\","
                + "\"status\":\"active\",\"cancel_at_period_end\":false}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_upd_nf\",\"type\":\"customer.subscription.updated\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_nobody")).thenReturn(Optional.empty());

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void handleWebhookInvoicePaymentFailedSetsUserFreeAndPastDueTest() throws Exception {
        user.setSubscription(Subscription.PLUS);
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_123\"}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_3\",\"type\":\"invoice.payment_failed\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_123")).thenReturn(Optional.of(user));

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Subscription.FREE, user.getSubscription());
        assertEquals("past_due", user.getSubscriptionStatus());
    }

    @Test
    void handleWebhookInvoicePaymentFailedUserNotFoundNoSaveTest() throws Exception {
        String payload = "{\"data\":{\"object\":{\"customer\":\"cus_ghost\"}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_fail_ghost\",\"type\":\"invoice.payment_failed\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);
        when(userRepository.findByStripeCustomerId("cus_ghost")).thenReturn(Optional.empty());

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void handleWebhookUnknownEventTypeReturnsOkTest() throws Exception {
        String payload = "{\"data\":{\"object\":{}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_4\",\"type\":\"unknown.event\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), any())).thenReturn(event);

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void handleWebhookGenericExceptionReturnsOkTest() throws Exception {
        when(stripeService.constructWebhookEvent(any(), any()))
                .thenThrow(new RuntimeException("unexpected error"));

        ResponseEntity<String> result = subscriptionController.handleWebhook("{}", "sig");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void handleWebhookNullSignatureHeaderStillProcessesWithNoSecretTest() throws Exception {
        String payload = "{\"data\":{\"object\":{}}}";
        Event event = new Gson().fromJson("{\"id\":\"evt_nullsig\",\"type\":\"unknown.event\"}", Event.class);
        when(stripeService.constructWebhookEvent(eq(payload), eq(null))).thenReturn(event);

        ResponseEntity<String> result = subscriptionController.handleWebhook(payload, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
