package com.wavii.service;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.Event;
import com.stripe.model.InvoicePayment;
import com.stripe.model.InvoicePaymentCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Refund;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.InvoicePaymentListParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.wavii.model.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para la integración con Stripe.
 * Gestiona pagos, suscripciones, webhooks y la configuración del Payment Sheet.
 * 
 * @author eduglezexp
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.price-plus:}")
    private String pricePlus;

    @Value("${stripe.price-scholar:}")
    private String priceScholar;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.coupon-scholar-promo:}")
    private String couponScholarPromo;

    /**
     * Inicializa la configuración de Stripe con la clave secreta.
     */
    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
            log.info("Stripe configurado correctamente");
        } else {
            log.warn("[DEV] Stripe no configurado - pagos en modo mock");
        }
    }

    /**
     * Comprueba si Stripe está configurado correctamente en el entorno.
     * 
     * @return true si hay una clave secreta configurada.
     */
    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    /**
     * Obtiene el ID de cliente de Stripe para un usuario o crea uno nuevo si no existe.
     * 
     * @param user Usuario del sistema.
     * @return ID del cliente en Stripe.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public String createOrGetCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            return user.getStripeCustomerId();
        }
        Customer customer = Customer.create(
                CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(user.getName())
                        .build());
        return customer.getId();
    }

    /**
     * Crea una suscripción recurrente en Stripe.
     * 
     * @param customerId ID del cliente en Stripe.
     * @param plan Nombre del plan (plus o scholar).
     * @param paymentMethodId ID del método de pago de Stripe.
     * @param trialUsed Indica si el usuario ya ha consumido el periodo de prueba.
     * @return Mapa con los detalles de la suscripción creada.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public Map<String, Object> createSubscription(String customerId, String plan,
                                                  String paymentMethodId, boolean trialUsed)
            throws StripeException {

        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
        pm.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());

        Customer customer = Customer.retrieve(customerId);
        customer.update(
                CustomerUpdateParams.builder()
                        .setInvoiceSettings(
                                CustomerUpdateParams.InvoiceSettings.builder()
                                        .setDefaultPaymentMethod(paymentMethodId)
                                        .build())
                        .build());

        String priceId = "plus".equals(plan) ? pricePlus : priceScholar;

        SubscriptionCreateParams.Builder builder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                .setDefaultPaymentMethod(paymentMethodId)
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .addExpand("latest_invoice");

        if ("plus".equals(plan) && !trialUsed) {
            builder.setTrialPeriodDays(14L);
        }

        Subscription subscription = Subscription.create(builder.build());

        boolean needsImmediateCharge = !"plus".equals(plan) || trialUsed;
        if (needsImmediateCharge) {
            String invoiceId = subscription.getLatestInvoice();
            if (invoiceId != null) {
                InvoicePaymentCollection invoicePayments = InvoicePayment.list(
                        InvoicePaymentListParams.builder()
                                .setInvoice(invoiceId)
                                .setLimit(1L)
                                .build());
                if (invoicePayments.getData() != null && !invoicePayments.getData().isEmpty()) {
                    String piId = invoicePayments.getData().get(0).getPayment().getPaymentIntent();
                    if (piId != null) {
                        PaymentIntent pi = PaymentIntent.retrieve(piId);
                        if (!"succeeded".equals(pi.getStatus())) {
                            pi.confirm(PaymentIntentConfirmParams.builder()
                                    .setPaymentMethod(paymentMethodId)
                                    .build());
                            subscription = Subscription.retrieve(subscription.getId());
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("subscriptionId", subscription.getId());
        result.put("status", subscription.getStatus());
        result.put("cancelAtPeriodEnd", subscription.getCancelAtPeriodEnd());
        Long currentPeriodEnd = getCurrentPeriodEnd(subscription);
        if (currentPeriodEnd != null) {
            result.put("currentPeriodEnd",
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(currentPeriodEnd), ZoneOffset.UTC).toString());
        }
        return result;
    }

    /** Sobrecarga de createSubscription sin indicar trialUsed (por defecto false). */
    public Map<String, Object> createSubscription(String customerId, String plan, String paymentMethodId)
            throws StripeException {
        return createSubscription(customerId, plan, paymentMethodId, false);
    }

    /**
     * Programa una suscripción para cancelarse al final del periodo de facturación actual.
     * 
     * @param subscriptionId ID de la suscripción en Stripe.
     * @return Detalles de la suscripción actualizada.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public Map<String, Object> cancelAtPeriodEnd(String subscriptionId) throws StripeException {
        Subscription sub = Subscription.retrieve(subscriptionId);
        Subscription updated = sub.update(
                SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build());
        Map<String, Object> result = new HashMap<>();
        result.put("subscriptionId", updated.getId());
        result.put("status", updated.getStatus());
        result.put("cancelAtPeriodEnd", updated.getCancelAtPeriodEnd());
        Long currentPeriodEnd = getCurrentPeriodEnd(updated);
        if (currentPeriodEnd != null) {
            result.put("currentPeriodEnd",
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(currentPeriodEnd), ZoneOffset.UTC).toString());
        }
        return result;
    }

    /**
     * Reactiva una suscripción que estaba programada para cancelarse.
     * 
     * @param subscriptionId ID de la suscripción en Stripe.
     * @return Detalles de la suscripción actualizada.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public Map<String, Object> reactivateSubscription(String subscriptionId) throws StripeException {
        Subscription sub = Subscription.retrieve(subscriptionId);
        Subscription updated = sub.update(
                SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(false)
                        .build());
        Map<String, Object> result = new HashMap<>();
        result.put("subscriptionId", updated.getId());
        result.put("status", updated.getStatus());
        result.put("cancelAtPeriodEnd", false);
        Long currentPeriodEnd = getCurrentPeriodEnd(updated);
        if (currentPeriodEnd != null) {
            result.put("currentPeriodEnd",
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(currentPeriodEnd), ZoneOffset.UTC).toString());
        }
        return result;
    }

    /**
     * Cambia el plan de una suscripción activa.
     * 
     * @param subscriptionId ID de la suscripción en Stripe.
     * @param newPlan Nombre del nuevo plan.
     * @param applyScholarPromo Indica si se aplica cupón de descuento promocional.
     * @return Detalles de la suscripción actualizada.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public Map<String, Object> changeSubscription(String subscriptionId, String newPlan, boolean applyScholarPromo)
            throws StripeException {

        Subscription sub = Subscription.retrieve(subscriptionId);
        String currentItemId = sub.getItems().getData().get(0).getId();
        String newPriceId = "plus".equals(newPlan) ? pricePlus : priceScholar;

        SubscriptionUpdateParams.Builder builder = SubscriptionUpdateParams.builder()
                .addItem(SubscriptionUpdateParams.Item.builder()
                        .setId(currentItemId)
                        .setPrice(newPriceId)
                        .build())
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.NONE)
                .setCancelAtPeriodEnd(false);

        if (sub.getTrialEnd() != null && sub.getTrialEnd() > System.currentTimeMillis() / 1000) {
            builder.setTrialEnd(SubscriptionUpdateParams.TrialEnd.NOW);
        }

        if ("scholar".equals(newPlan) && applyScholarPromo
                && couponScholarPromo != null && !couponScholarPromo.isBlank()) {
            builder.addDiscount(
                    SubscriptionUpdateParams.Discount.builder()
                            .setCoupon(couponScholarPromo)
                            .build());
        }

        Subscription updated = sub.update(builder.build());

        Map<String, Object> result = new HashMap<>();
        result.put("subscriptionId", updated.getId());
        result.put("status", updated.getStatus());
        result.put("cancelAtPeriodEnd", updated.getCancelAtPeriodEnd());
        Long currentPeriodEnd = getCurrentPeriodEnd(updated);
        if (currentPeriodEnd != null) {
            result.put("currentPeriodEnd",
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(currentPeriodEnd), ZoneOffset.UTC).toString());
        }
        return result;
    }

    /**
     * Crea una clave efímera para el SDK de Stripe en el móvil.
     * 
     * @param customerId ID del cliente.
     * @param stripeVersion Versión de la API de Stripe requerida por el SDK.
     * @return Secreto de la clave efímera.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public String createEphemeralKey(String customerId, String stripeVersion) throws StripeException {
        EphemeralKey key = EphemeralKey.create(
                EphemeralKeyCreateParams.builder()
                        .setCustomer(customerId)
                        .setStripeVersion(stripeVersion)
                        .build()
                        .toMap(),
                (RequestOptions) null);
        return key.getSecret();
    }

    /**
     * Crea un SetupIntent para recoger un método de pago sin cobrar inmediatamente.
     * 
     * @param customerId ID del cliente.
     * @param plan Nombre del plan asociado.
     * @return Mapa con el ID y secreto del SetupIntent.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public Map<String, Object> createSetupIntent(String customerId, String plan) throws StripeException {
        SetupIntent si = SetupIntent.create(
                SetupIntentCreateParams.builder()
                        .setCustomer(customerId)
                        .addPaymentMethodType("card")
                        .putMetadata("plan", plan)
                        .build());
        Map<String, Object> result = new HashMap<>();
        result.put("setupIntentId", si.getId());
        result.put("clientSecret", si.getClientSecret());
        return result;
    }

    /**
     * Crea un PaymentIntent para el pago de una hora de clase.
     * 
     * @param customerId ID del cliente.
     * @param teacherId ID del profesor.
     * @param teacherName Nombre del profesor.
     * @param studentName Nombre del alumno.
     * @param instrument Instrumento de la clase.
     * @param modality Modalidad (presencial/online).
     * @param city Ciudad.
     * @param amountCents Importe en céntimos de euro.
     * @return Mapa con el ID y secreto del PaymentIntent.
     * @throws StripeException Si hay un error con la API de Stripe.
     */
    public Map<String, Object> createClassPaymentIntent(String customerId, String teacherId, String teacherName,
                                                        String studentName, String instrument, String modality,
                                                        String city, long amountCents) throws StripeException {
        PaymentIntent intent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency("eur")
                        .setCustomer(customerId)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build())
                        .putMetadata("teacherId", teacherId)
                        .putMetadata("teacherName", teacherName)
                        .putMetadata("studentName", studentName)
                        .putMetadata("instrument", instrument != null ? instrument : "")
                        .putMetadata("modality", modality != null ? modality : "")
                        .putMetadata("city", city != null ? city : "")
                        .putMetadata("kind", "class_payment")
                        .build());

        Map<String, Object> result = new HashMap<>();
        result.put("paymentIntentId", intent.getId());
        result.put("clientSecret", intent.getClientSecret());
        return result;
    }

    /** Obtiene los detalles de un PaymentIntent a partir de su ID. */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /** Realiza el reembolso completo de un PaymentIntent. */
    public Refund refundPaymentIntent(String paymentIntentId) throws StripeException {
        return Refund.create(RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build());
    }

    /** Crea una suscripción utilizando un SetupIntent previo. */
    public Map<String, Object> createSubscriptionFromSetupIntent(
            String customerId, String setupIntentId, String plan, boolean trialUsed) throws StripeException {
        SetupIntent si = SetupIntent.retrieve(setupIntentId);
        String paymentMethodId = si.getPaymentMethod();
        return createSubscription(customerId, plan, paymentMethodId, trialUsed);
    }

    /** Sobrecarga de createSubscriptionFromSetupIntent sin indicar trialUsed. */
    public Map<String, Object> createSubscriptionFromSetupIntent(
            String customerId, String setupIntentId, String plan) throws StripeException {
        return createSubscriptionFromSetupIntent(customerId, setupIntentId, plan, false);
    }

    /**
     * Valida la firma de un evento de webhook de Stripe y lo convierte en objeto Event.
     * 
     * @param payload Cuerpo JSON del webhook.
     * @param sigHeader Cabecera de firma de Stripe.
     * @return El objeto Event validado.
     * @throws SignatureVerificationException Si la firma no es válida.
     */
    public Event constructWebhookEvent(String payload, String sigHeader) throws SignatureVerificationException {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        }
        log.warn("STRIPE_WEBHOOK_SECRET no configurado - saltando verificacion de firma (modo dev)");
        return new Gson().fromJson(payload, Event.class);
    }

    public String getPricePlus() {
        return pricePlus;
    }

    public String getPriceScholar() {
        return priceScholar;
    }

    private Long getCurrentPeriodEnd(Subscription subscription) {
        if (subscription.getItems() == null || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()) {
            return null;
        }
        return subscription.getItems().getData().get(0).getCurrentPeriodEnd();
    }
}
