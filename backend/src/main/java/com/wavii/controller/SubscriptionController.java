package com.wavii.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.wavii.model.User;
import com.wavii.model.enums.Subscription;
import com.wavii.repository.UserRepository;
import com.wavii.service.OdooService;
import com.wavii.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final StripeService stripeService;
    private final UserRepository userRepository;
    private final OdooService odooService;

    private static final String STRIPE_API_VERSION = "2023-10-16";

    @PostMapping("/setup-intent")
    public ResponseEntity<?> createSetupIntent(
            @RequestBody SetupIntentRequest req,
            Principal principal) {

        User user = getUser(principal);

        if (!stripeService.isConfigured()) {
            return ResponseEntity.ok(Map.of(
                    "ephemeralKey", "ek_test_dev_mock",
                    "setupIntentClientSecret", "seti_dev_mock_secret",
                    "customerId", "cus_dev_mock",
                    "publishableKey", "",
                    "trialUsed", user.isTrialUsed(),
                    "devMode", true
            ));
        }

        try {
            String customerId = stripeService.createOrGetCustomer(user);
            user.setStripeCustomerId(customerId);
            userRepository.save(user);

            String ephemeralKey = stripeService.createEphemeralKey(customerId, STRIPE_API_VERSION);
            Map<String, Object> siResult = stripeService.createSetupIntent(customerId, req.plan());

            return ResponseEntity.ok(Map.of(
                    "ephemeralKey", ephemeralKey,
                    "setupIntentClientSecret", siResult.get("clientSecret"),
                    "setupIntentId", siResult.get("setupIntentId"),
                    "customerId", customerId,
                    "trialUsed", user.isTrialUsed()
            ));
        } catch (Exception e) {
            log.error("Error creando SetupIntent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al preparar el pago: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSubscription(
            @RequestBody ConfirmSubscriptionRequest req,
            Principal principal) {

        User user = getUser(principal);

        if (!stripeService.isConfigured()) {
            Subscription sub = toSubscriptionEnum(req.plan());
            user.setSubscription(sub);
            user.setSubscriptionStatus("active");
            user.setStripeSubscriptionId("dev_sub_" + System.currentTimeMillis());
            if ("plus".equals(req.plan())) user.setTrialUsed(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "subscriptionId", user.getStripeSubscriptionId(),
                    "status", "active",
                    "devMode", true
            ));
        }

        try {
            String customerId = user.getStripeCustomerId();
            Map<String, Object> result = stripeService.createSubscriptionFromSetupIntent(
                    customerId, req.setupIntentId(), req.plan(), user.isTrialUsed());

            applySubscriptionResult(user, req.plan(), result);
            userRepository.save(user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error confirmando suscripcion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al activar la suscripcion: " + e.getMessage()));
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> startSubscription(
            @RequestBody StartSubscriptionRequest req,
            Principal principal) {

        User user = getUser(principal);

        if (!stripeService.isConfigured()) {
            Subscription sub = toSubscriptionEnum(req.plan());
            user.setSubscription(sub);
            user.setSubscriptionStatus("active");
            user.setStripeSubscriptionId("dev_sub_" + System.currentTimeMillis());
            if ("plus".equals(req.plan())) user.setTrialUsed(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "subscriptionId", user.getStripeSubscriptionId(),
                    "status", "active",
                    "devMode", true
            ));
        }

        try {
            String customerId = stripeService.createOrGetCustomer(user);
            user.setStripeCustomerId(customerId);
            userRepository.save(user);

            Map<String, Object> result = stripeService.createSubscription(
                    customerId, req.plan(), req.paymentMethodId(), user.isTrialUsed());

            applySubscriptionResult(user, req.plan(), result);
            userRepository.save(user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error al crear suscripcion Stripe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al procesar el pago: " + e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(Principal principal) {
        User user = getUser(principal);

        if (user.getSubscription() == Subscription.FREE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No tienes ninguna suscripción activa que cancelar"));
        }

        if (!stripeService.isConfigured()) {
            user.setSubscriptionCancelAtPeriodEnd(true);
            user.setSubscriptionStatus("cancel_at_period_end");
            LocalDateTime currentPeriodEnd = LocalDateTime.now().plusDays(30);
            user.setSubscriptionCurrentPeriodEnd(currentPeriodEnd);
            userRepository.save(user);
            odooService.createSubscriptionTask(
                    user.getName(),
                    user.getEmail(),
                    "Cancelación programada de suscripción",
                    "Plan actual: " + user.getSubscription().name() + "\nFin de periodo: " + currentPeriodEnd
            );
            return ResponseEntity.ok(Map.of(
                    "cancelAtPeriodEnd", true,
                    "currentPeriodEnd", currentPeriodEnd.toString(),
                    "devMode", true
            ));
        }

        if (user.getStripeSubscriptionId() == null || user.getStripeSubscriptionId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No se encontró ninguna suscripción activa en Stripe"));
        }

        try {
            Map<String, Object> result = stripeService.cancelAtPeriodEnd(user.getStripeSubscriptionId());

            user.setSubscriptionCancelAtPeriodEnd(true);
            user.setSubscriptionStatus((String) result.getOrDefault("status", user.getSubscriptionStatus()));
            if (result.containsKey("currentPeriodEnd")) {
                user.setSubscriptionCurrentPeriodEnd(
                        LocalDateTime.parse((String) result.get("currentPeriodEnd")));
            }
            userRepository.save(user);
            odooService.createSubscriptionTask(
                    user.getName(),
                    user.getEmail(),
                    "Cancelación programada de suscripción",
                    "Plan actual: " + user.getSubscription().name()
                            + "\nFin de periodo: " + result.getOrDefault("currentPeriodEnd", "no informado")
            );

            log.info("Suscripción de {} programada para cancelar al final del periodo", user.getEmail());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error cancelando suscripcion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al cancelar la suscripción: " + e.getMessage()));
        }
    }

    @PostMapping("/reactivate")
    public ResponseEntity<?> reactivateSubscription(Principal principal) {
        User user = getUser(principal);

        if (!user.isSubscriptionCancelAtPeriodEnd()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Tu suscripción no está pendiente de cancelación"));
        }

        if (!stripeService.isConfigured()) {
            user.setSubscriptionCancelAtPeriodEnd(false);
            user.setSubscriptionStatus("active");
            userRepository.save(user);
            odooService.createSubscriptionTask(
                    user.getName(),
                    user.getEmail(),
                    "Reactivación de suscripción",
                    "Plan: " + user.getSubscription().name() + "\nEstado Stripe: active"
            );
            return ResponseEntity.ok(Map.of("cancelAtPeriodEnd", false, "devMode", true));
        }

        try {
            Map<String, Object> result = stripeService.reactivateSubscription(user.getStripeSubscriptionId());
            user.setSubscriptionCancelAtPeriodEnd(false);
            user.setSubscriptionStatus((String) result.getOrDefault("status", user.getSubscriptionStatus()));
            if (result.containsKey("currentPeriodEnd") && result.get("currentPeriodEnd") != null) {
                user.setSubscriptionCurrentPeriodEnd(LocalDateTime.parse((String) result.get("currentPeriodEnd")));
            }
            userRepository.save(user);
            odooService.createSubscriptionTask(
                    user.getName(),
                    user.getEmail(),
                    "Reactivación de suscripción",
                    "Plan: " + user.getSubscription().name() + "\nEstado Stripe: " + user.getSubscriptionStatus()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error reactivando suscripcion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al reactivar la suscripción: " + e.getMessage()));
        }
    }

    @PostMapping("/change")
    public ResponseEntity<?> changeSubscription(
            @RequestBody ChangeSubscriptionRequest req,
            Principal principal) {

        User user = getUser(principal);

        if (user.getStripeSubscriptionId() == null || user.getStripeSubscriptionId().isBlank()) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of(
                            "needsPaymentSheet", true,
                            "trialUsed", user.isTrialUsed(),
                            "message", "Es necesario añadir un método de pago"
                    ));
        }

        if (!stripeService.isConfigured()) {
            Subscription previousPlan = user.getSubscription();
            Subscription sub = toSubscriptionEnum(req.plan());
            boolean promoApplied = shouldApplyScholarPromo(user, req.plan());
            user.setSubscription(sub);
            user.setSubscriptionStatus("active");
            user.setSubscriptionCancelAtPeriodEnd(false);
            userRepository.save(user);
            odooService.createSubscriptionTask(
                    user.getName(),
                    user.getEmail(),
                    "Cambio de suscripción: " + previousPlan.name() + " -> " + user.getSubscription().name(),
                    "Nuevo plan: " + user.getSubscription().name()
                            + "\nPromo aplicada: " + (promoApplied ? "sí" : "no")
                            + "\nEstado Stripe: active"
            );
            return ResponseEntity.ok(Map.of(
                    "subscription", req.plan(),
                    "status", "active",
                    "cancelAtPeriodEnd", false,
                    "promoApplied", promoApplied,
                    "devMode", true
            ));
        }

        try {
            Subscription previousPlan = user.getSubscription();
            boolean promoApplied = shouldApplyScholarPromo(user, req.plan());
            Map<String, Object> result = stripeService.changeSubscription(
                    user.getStripeSubscriptionId(), req.plan(), promoApplied);

            user.setSubscription(toSubscriptionEnum(req.plan()));
            user.setSubscriptionStatus((String) result.get("status"));
            user.setSubscriptionCancelAtPeriodEnd(Boolean.TRUE.equals(result.get("cancelAtPeriodEnd")));
            if (result.containsKey("currentPeriodEnd") && result.get("currentPeriodEnd") != null) {
                user.setSubscriptionCurrentPeriodEnd(
                        LocalDateTime.parse((String) result.get("currentPeriodEnd")));
            }
            userRepository.save(user);
            odooService.createSubscriptionTask(
                    user.getName(),
                    user.getEmail(),
                    "Cambio de suscripción: " + previousPlan.name() + " -> " + user.getSubscription().name(),
                    "Nuevo plan: " + user.getSubscription().name()
                            + "\nPromo aplicada: " + (promoApplied ? "sí" : "no")
                            + "\nEstado Stripe: " + user.getSubscriptionStatus()
            );

            log.info("Plan de {} cambiado a {}", user.getEmail(), req.plan());
            return ResponseEntity.ok(Map.of(
                    "subscriptionId", result.get("subscriptionId"),
                    "status", result.get("status"),
                    "cancelAtPeriodEnd", result.get("cancelAtPeriodEnd"),
                    "currentPeriodEnd", result.get("currentPeriodEnd"),
                    "promoApplied", promoApplied
            ));

        } catch (Exception e) {
            log.error("Error cambiando suscripcion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error al cambiar el plan: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(Map.of(
                "subscription", user.getSubscription().name().toLowerCase(),
                "subscriptionStatus", user.getSubscriptionStatus() != null ? user.getSubscriptionStatus() : "none",
                "stripeSubscriptionId", user.getStripeSubscriptionId() != null ? user.getStripeSubscriptionId() : "",
                "cancelAtPeriodEnd", user.isSubscriptionCancelAtPeriodEnd(),
                "currentPeriodEnd", user.getSubscriptionCurrentPeriodEnd() != null
                        ? user.getSubscriptionCurrentPeriodEnd().toString() : "",
                "trialUsed", user.isTrialUsed(),
                "deletionScheduledAt", user.getDeletionScheduledAt() != null
                        ? user.getDeletionScheduledAt().toString() : ""
        ));
    }

    @PostMapping("/webhook")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        try {
            Event event = stripeService.constructWebhookEvent(payload, sigHeader);
            log.info("Webhook Stripe recibido: {}", event.getType());

            Map<String, Object> payloadMap = new ObjectMapper().readValue(payload, Map.class);
            Map<String, Object> data = (Map<String, Object>) payloadMap.get("data");
            Map<String, Object> obj = (Map<String, Object>) data.get("object");

            switch (event.getType()) {
                case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(obj);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(obj);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(obj);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(obj);
            }

            return ResponseEntity.ok("OK");

        } catch (SignatureVerificationException e) {
            log.error("Firma de webhook invalida: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage());
            return ResponseEntity.ok("OK");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleInvoicePaymentSucceeded(Map<String, Object> invoice) {
        String customerId = (String) invoice.get("customer");
        Number amountPaid = (Number) invoice.get("amount_paid");
        double amount = amountPaid != null ? amountPaid.doubleValue() / 100.0 : 0.0;

        String planNameRaw = "scholar";
        try {
            Map<String, Object> lines = (Map<String, Object>) invoice.get("lines");
            List<Map<String, Object>> lineData = (List<Map<String, Object>>) lines.get("data");
            if (!lineData.isEmpty()) {
                String desc = (String) lineData.get(0).get("description");
                if (desc != null && !desc.isBlank()) planNameRaw = desc;
            }
        } catch (Exception ignored) {
        }
        final String planName = planNameRaw;

        userRepository.findByStripeCustomerId(customerId).ifPresentOrElse(user -> {
            user.setSubscriptionStatus("active");
            if (user.getSubscription() == Subscription.PLUS && !user.isTrialUsed()) {
                user.setTrialUsed(true);
            }
            userRepository.save(user);
            log.info("Pago exitoso para {} - {}EUR", user.getEmail(), amount);
            odooService.processStripePayment(user.getEmail(), user.getName(), planName, amount);
        }, () -> log.warn("Webhook: no se encontro usuario con customerId={}", customerId));
    }

    @SuppressWarnings("unchecked")
    private void handleSubscriptionUpdated(Map<String, Object> sub) {
        String customerId = (String) sub.get("customer");
        String status = (String) sub.get("status");
        Boolean cancelAtEnd = (Boolean) sub.get("cancel_at_period_end");
        Number periodEnd = (Number) sub.get("current_period_end");

        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            if (status != null) user.setSubscriptionStatus(status);
            if (cancelAtEnd != null) user.setSubscriptionCancelAtPeriodEnd(cancelAtEnd);
            if (periodEnd != null) {
                user.setSubscriptionCurrentPeriodEnd(
                        LocalDateTime.ofEpochSecond(periodEnd.longValue(), 0, java.time.ZoneOffset.UTC));
            }
            userRepository.save(user);
            log.info("Suscripción actualizada para {} - status={}, cancelAtEnd={}",
                    user.getEmail(), status, cancelAtEnd);
        });
    }

    private void handleSubscriptionDeleted(Map<String, Object> sub) {
        String customerId = (String) sub.get("customer");
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setSubscription(Subscription.FREE);
            user.setSubscriptionStatus("canceled");
            user.setSubscriptionCancelAtPeriodEnd(false);
            user.setStripeSubscriptionId(null);
            userRepository.save(user);
            log.info("Suscripción eliminada para {} - bajado a FREE", user.getEmail());
        });
    }

    private void handleInvoicePaymentFailed(Map<String, Object> invoice) {
        String customerId = (String) invoice.get("customer");
        userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
            user.setSubscription(Subscription.FREE);
            user.setSubscriptionStatus("past_due");
            userRepository.save(user);
            log.warn("Pago fallido para {} - bajado a FREE", user.getEmail());
        });
    }

    private User getUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Subscription toSubscriptionEnum(String plan) {
        return "scholar".equals(plan) ? Subscription.EDUCATION : Subscription.PLUS;
    }

    private boolean shouldApplyScholarPromo(User user, String targetPlan) {
        return "scholar".equals(targetPlan)
                && user.getSubscription() == Subscription.PLUS
                && !"trialing".equalsIgnoreCase(user.getSubscriptionStatus());
    }

    private void applySubscriptionResult(User user, String plan, Map<String, Object> result) {
        user.setStripeSubscriptionId((String) result.get("subscriptionId"));
        String status = (String) result.get("status");
        user.setSubscriptionStatus(status);
        if ("trialing".equals(status) || "active".equals(status)) {
            user.setSubscription(toSubscriptionEnum(plan));
        }
        if ("plus".equals(plan)) user.setTrialUsed(true);
        if (result.containsKey("cancelAtPeriodEnd")) {
            user.setSubscriptionCancelAtPeriodEnd(Boolean.TRUE.equals(result.get("cancelAtPeriodEnd")));
        }
        if (result.containsKey("currentPeriodEnd") && result.get("currentPeriodEnd") != null) {
            try {
                user.setSubscriptionCurrentPeriodEnd(
                        LocalDateTime.parse((String) result.get("currentPeriodEnd")));
            } catch (Exception ignored) {
            }
        }
    }

    record SetupIntentRequest(String plan) {}
    record ConfirmSubscriptionRequest(String plan, String setupIntentId) {}
    record StartSubscriptionRequest(String plan, String paymentMethodId) {}
    record ChangeSubscriptionRequest(String plan) {}
}
