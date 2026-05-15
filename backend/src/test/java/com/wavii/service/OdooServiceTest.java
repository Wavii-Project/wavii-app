package com.wavii.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
class OdooServiceTest {

    @InjectMocks
    private OdooService odooService;

    @Mock
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "");
        ReflectionTestUtils.setField(odooService, "odooDb", "");
        ReflectionTestUtils.setField(odooService, "odooUser", "");
        ReflectionTestUtils.setField(odooService, "odooPassword", "admin_pass");
    }

    private void configureOdoo() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "http://odoo.local:8069");
        ReflectionTestUtils.setField(odooService, "odooDb", "wavii_db");
        ReflectionTestUtils.setField(odooService, "odooUser", "admin@wavii.com");
        ReflectionTestUtils.setField(odooService, "httpClient", mockHttpClient);
    }

    private HttpResponse buildResponse(String body) {
        HttpResponse resp = mock(HttpResponse.class);
        lenient().when(resp.body()).thenReturn(body);
        return resp;
    }

    // ── isConfigured ──────────────────────────────────────────────

    @Test
    void isConfiguredAllFieldsSetReturnsTrueTest() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "http://odoo.local:8069");
        ReflectionTestUtils.setField(odooService, "odooDb", "wavii_db");
        ReflectionTestUtils.setField(odooService, "odooUser", "admin@wavii.com");
        assertTrue(odooService.isConfigured());
    }

    @Test
    void isConfiguredUrlBlankReturnsFalseTest() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "");
        ReflectionTestUtils.setField(odooService, "odooDb", "wavii_db");
        ReflectionTestUtils.setField(odooService, "odooUser", "admin");
        assertFalse(odooService.isConfigured());
    }

    @Test
    void isConfiguredUrlNullReturnsFalseTest() {
        ReflectionTestUtils.setField(odooService, "odooUrl", null);
        ReflectionTestUtils.setField(odooService, "odooDb", "wavii_db");
        ReflectionTestUtils.setField(odooService, "odooUser", "admin");
        assertFalse(odooService.isConfigured());
    }

    @Test
    void isConfiguredDbBlankReturnsFalseTest() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "http://odoo.local:8069");
        ReflectionTestUtils.setField(odooService, "odooDb", "");
        ReflectionTestUtils.setField(odooService, "odooUser", "admin");
        assertFalse(odooService.isConfigured());
    }

    @Test
    void isConfiguredDbNullReturnsFalseTest() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "http://odoo.local:8069");
        ReflectionTestUtils.setField(odooService, "odooDb", null);
        ReflectionTestUtils.setField(odooService, "odooUser", "admin");
        assertFalse(odooService.isConfigured());
    }

    @Test
    void isConfiguredUserBlankReturnsFalseTest() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "http://odoo.local:8069");
        ReflectionTestUtils.setField(odooService, "odooDb", "wavii_db");
        ReflectionTestUtils.setField(odooService, "odooUser", "");
        assertFalse(odooService.isConfigured());
    }

    @Test
    void isConfiguredUserNullReturnsFalseTest() {
        ReflectionTestUtils.setField(odooService, "odooUrl", "http://odoo.local:8069");
        ReflectionTestUtils.setField(odooService, "odooDb", "wavii_db");
        ReflectionTestUtils.setField(odooService, "odooUser", null);
        assertFalse(odooService.isConfigured());
    }

    // ── Not configured ─────────────────────────────────────────────

    @Test
    void createCrmContactNotConfiguredDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.createCrmContact("Test User", "test@test.com", "USUARIO", "FREE"));
    }

    @Test
    void createCrmContactNotConfiguredNullNameDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.createCrmContact(null, "test@test.com", "USUARIO", "FREE"));
    }

    @Test
    void createCrmContactNotConfiguredBlankNameDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.createCrmContact("", "test@test.com", "PROFESOR_PARTICULAR", "PLUS"));
    }

    @Test
    void processStripePaymentNotConfiguredDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.processStripePayment("customer@test.com", "Customer Name", "plus", 9.99));
    }

    @Test
    void processStripePaymentNotConfiguredZeroAmountDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.processStripePayment("customer@test.com", "Customer", "scholar", 0.0));
    }

    @Test
    void createVerificationTaskNotConfiguredDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.createVerificationTask("Prof. Test", "prof@test.com",
                        "certificate.pdf", "http://localhost:8080/uploads/cert.pdf"));
    }

    @Test
    void createVerificationTaskNotConfiguredNullDocumentUrlDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.createVerificationTask("Prof. Test", "prof@test.com",
                        "certificate.pdf", null));
    }

    @Test
    void createSubscriptionTaskNotConfiguredDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.createSubscriptionTask("User A", "user@test.com",
                        "Nueva suscripción Scholar", "El usuario se ha suscrito al plan Scholar"));
    }

    @Test
    void processClassPaymentNotConfiguredDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.processClassPayment("student@test.com", "Student A",
                        "Prof. Ana", "Piano", "ONLINE", "Madrid", 40.0));
    }

    @Test
    void processClassRefundNotConfiguredDoesNotThrowTest() {
        assertDoesNotThrow(() ->
                odooService.processClassRefund("student@test.com", "Student A",
                        "Prof. Ana", 40.0));
    }

    // ── Configured — createCrmContact ─────────────────────────────

    @Test
    void createCrmContactConfiguredPartnerNotFoundCreatesNewPartnerTest() throws Exception {
        configureOdoo();
        HttpResponse authResp   = buildResponse("{\"result\": 1}");
        HttpResponse searchResp = buildResponse("{\"result\": []}");
        HttpResponse createResp = buildResponse("{\"result\": 99}");

        doReturn(authResp).doReturn(searchResp).doReturn(createResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createCrmContact("Ana", "ana@test.com", "USUARIO", "FREE"));
        verify(mockHttpClient, times(3)).send(any(), any());
    }

    @Test
    void createCrmContactConfiguredPartnerAlreadyExistsSkipsCreationTest() throws Exception {
        configureOdoo();
        HttpResponse authResp   = buildResponse("{\"result\": 1}");
        HttpResponse searchResp = buildResponse("{\"result\": [42]}");

        doReturn(authResp).doReturn(searchResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createCrmContact("Ana", "ana@test.com", "USUARIO", "FREE"));
        verify(mockHttpClient, times(2)).send(any(), any());
    }

    @Test
    void createCrmContactConfiguredNullNameUsesEmailAsNameTest() throws Exception {
        configureOdoo();
        HttpResponse authResp   = buildResponse("{\"result\": 1}");
        HttpResponse searchResp = buildResponse("{\"result\": []}");
        HttpResponse createResp = buildResponse("{\"result\": 99}");

        doReturn(authResp).doReturn(searchResp).doReturn(createResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createCrmContact(null, "ana@test.com", "USUARIO", "FREE"));
    }

    @Test
    void createCrmContactConfiguredAuthFailsLogsWarningTest() throws Exception {
        configureOdoo();
        HttpResponse authResp = buildResponse("{\"result\": false}");

        doReturn(authResp).when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createCrmContact("Ana", "ana@test.com", "USUARIO", "FREE"));
    }

    @Test
    void createCrmContactConfiguredHttpExceptionLogsWarningTest() throws Exception {
        configureOdoo();
        doThrow(new java.io.IOException("Connection refused"))
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createCrmContact("Ana", "ana@test.com", "USUARIO", "FREE"));
    }

    @Test
    void createCrmContactConfiguredRpcErrorLogsWarningTest() throws Exception {
        configureOdoo();
        HttpResponse authResp = buildResponse("{\"result\": 1}");
        HttpResponse errorResp = buildResponse("{\"error\": {\"message\": \"Access denied\"}}");

        doReturn(authResp).doReturn(errorResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createCrmContact("Ana", "ana@test.com", "USUARIO", "FREE"));
    }

    // ── Configured — processStripePayment ─────────────────────────

    @Test
    void processStripePaymentConfiguredFullFlowSuccessTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": []}");
        HttpResponse createPartnerResp = buildResponse("{\"result\": 99}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": [1]}");
        HttpResponse createInvoiceResp = buildResponse("{\"result\": 200}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": [1]}");
        HttpResponse createPaymentResp = buildResponse("{\"result\": 300}");
        HttpResponse postPaymentResp   = buildResponse("{\"result\": true}");
        HttpResponse reconcileResp     = buildResponse("{\"result\": null}");

        doReturn(authResp)
                .doReturn(searchPartnerResp).doReturn(createPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp).doReturn(createPaymentResp).doReturn(postPaymentResp)
                .doReturn(reconcileResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processStripePayment("customer@test.com", "Customer", "plus", 9.99));
    }

    @Test
    void processStripePaymentConfiguredExistingPartnerNoCurrencyTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": [42]}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": []}");
        HttpResponse createInvoiceResp = buildResponse("{\"result\": [200]}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": []}");

        doReturn(authResp)
                .doReturn(searchPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processStripePayment("customer@test.com", "Customer", "scholar", 4.99));
    }

    @Test
    void processStripePaymentConfiguredReconcileErrorLogsWarningTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": [42]}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": [1]}");
        HttpResponse createInvoiceResp = buildResponse("{\"result\": 200}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": [1]}");
        HttpResponse createPaymentResp = buildResponse("{\"result\": 300}");
        HttpResponse postPaymentResp   = buildResponse("{\"result\": true}");
        HttpResponse reconcileErrResp  = buildResponse("{\"error\": {\"message\": \"reconcile error\"}}");

        doReturn(authResp)
                .doReturn(searchPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp).doReturn(createPaymentResp).doReturn(postPaymentResp)
                .doReturn(reconcileErrResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processStripePayment("customer@test.com", "Customer", "plus", 9.99));
    }

    @Test
    void processStripePaymentConfiguredHttpExceptionLogsErrorTest() throws Exception {
        configureOdoo();
        doThrow(new java.io.IOException("Connection refused"))
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processStripePayment("customer@test.com", "Customer", "plus", 9.99));
    }

    @Test
    void processStripePaymentConfiguredInvoiceAsListResultTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": [42]}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": [1]}");
        // invoice returned as a list (Odoo 17 style)
        HttpResponse createInvoiceResp = buildResponse("{\"result\": [200]}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": [1]}");
        HttpResponse createPaymentResp = buildResponse("{\"result\": [300]}");
        HttpResponse postPaymentResp   = buildResponse("{\"result\": true}");
        HttpResponse reconcileResp     = buildResponse("{\"result\": null}");

        doReturn(authResp)
                .doReturn(searchPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp).doReturn(createPaymentResp).doReturn(postPaymentResp)
                .doReturn(reconcileResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processStripePayment("customer@test.com", "Customer", "plus", 9.99));
    }

    // ── Configured — createVerificationTask ──────────────────────

    @Test
    void createVerificationTaskConfiguredProjectNotFoundCreatesProjectAndTaskTest() throws Exception {
        configureOdoo();
        HttpResponse authResp       = buildResponse("{\"result\": 1}");
        HttpResponse searchProjResp = buildResponse("{\"result\": []}");
        HttpResponse createProjResp = buildResponse("{\"result\": 10}");
        HttpResponse createTaskResp = buildResponse("{\"result\": 20}");

        doReturn(authResp).doReturn(searchProjResp).doReturn(createProjResp).doReturn(createTaskResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createVerificationTask("Prof. Ana", "ana@test.com",
                        "certificate.pdf", "http://localhost/cert.pdf"));
    }

    @Test
    void createVerificationTaskConfiguredProjectExistsCreatesTaskTest() throws Exception {
        configureOdoo();
        HttpResponse authResp       = buildResponse("{\"result\": 1}");
        HttpResponse searchProjResp = buildResponse("{\"result\": [5]}");
        HttpResponse createTaskResp = buildResponse("{\"result\": 20}");

        doReturn(authResp).doReturn(searchProjResp).doReturn(createTaskResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createVerificationTask("Prof. Juan", "juan@test.com", "doc.pdf", null));
    }

    @Test
    void createVerificationTaskConfiguredHttpExceptionLogsWarningTest() throws Exception {
        configureOdoo();
        doThrow(new java.io.IOException("Timeout"))
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createVerificationTask("Prof. Test", "test@test.com",
                        "cert.pdf", "http://url.com/cert.pdf"));
    }

    @Test
    void createVerificationTaskConfiguredNullUrlIncludesNoUrlLineTest() throws Exception {
        configureOdoo();
        HttpResponse authResp       = buildResponse("{\"result\": 1}");
        HttpResponse searchProjResp = buildResponse("{\"result\": [5]}");
        HttpResponse createTaskResp = buildResponse("{\"result\": 20}");

        doReturn(authResp).doReturn(searchProjResp).doReturn(createTaskResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createVerificationTask("Prof. Test", "test@test.com", "cert.pdf", null));
    }

    // ── Configured — createSubscriptionTask ──────────────────────

    @Test
    void createSubscriptionTaskConfiguredProjectNotFoundCreatesProjectAndTaskTest() throws Exception {
        configureOdoo();
        HttpResponse authResp       = buildResponse("{\"result\": 1}");
        HttpResponse searchProjResp = buildResponse("{\"result\": []}");
        HttpResponse createProjResp = buildResponse("{\"result\": 15}");
        HttpResponse createTaskResp = buildResponse("{\"result\": 25}");

        doReturn(authResp).doReturn(searchProjResp).doReturn(createProjResp).doReturn(createTaskResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createSubscriptionTask("User A", "user@test.com",
                        "Suscripción Scholar", "El usuario ha adquirido Scholar"));
    }

    @Test
    void createSubscriptionTaskConfiguredProjectExistsCreatesTaskTest() throws Exception {
        configureOdoo();
        HttpResponse authResp       = buildResponse("{\"result\": 1}");
        HttpResponse searchProjResp = buildResponse("{\"result\": [8]}");
        HttpResponse createTaskResp = buildResponse("{\"result\": 30}");

        doReturn(authResp).doReturn(searchProjResp).doReturn(createTaskResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createSubscriptionTask("User B", "user2@test.com",
                        "Cancelación Scholar", "El usuario ha cancelado Scholar"));
    }

    @Test
    void createSubscriptionTaskConfiguredHttpExceptionLogsWarningTest() throws Exception {
        configureOdoo();
        doThrow(new java.io.IOException("Connection timeout"))
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createSubscriptionTask("User C", "user3@test.com",
                        "Suscripción Plus", "El usuario se suscribió a Plus"));
    }

    @Test
    void createSubscriptionTaskConfiguredAuthFailsLogsWarningTest() throws Exception {
        configureOdoo();
        HttpResponse authResp = buildResponse("{\"result\": false}");

        doReturn(authResp).when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.createSubscriptionTask("User D", "user4@test.com",
                        "Suscripción", "Detalles suscripción"));
    }

    // ── Configured — processClassPayment ─────────────────────────

    @Test
    void processClassPaymentConfiguredFullFlowSuccessTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": []}");
        HttpResponse createPartnerResp = buildResponse("{\"result\": 50}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": [1]}");
        HttpResponse createInvoiceResp = buildResponse("{\"result\": 100}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": [1]}");
        HttpResponse createPaymentResp = buildResponse("{\"result\": 200}");
        HttpResponse postPaymentResp   = buildResponse("{\"result\": true}");
        HttpResponse reconcileResp     = buildResponse("{\"result\": null}");

        doReturn(authResp)
                .doReturn(searchPartnerResp).doReturn(createPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp).doReturn(createPaymentResp).doReturn(postPaymentResp)
                .doReturn(reconcileResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processClassPayment("student@test.com", "Student A",
                        "Prof. Ana", "Piano", "ONLINE", "Madrid", 40.0));
    }

    @Test
    void processClassPaymentConfiguredHttpExceptionLogsErrorTest() throws Exception {
        configureOdoo();
        doThrow(new java.io.IOException("Connection refused"))
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processClassPayment("student@test.com", "Student A",
                        "Prof. Ana", "Piano", "PRESENCIAL", "Madrid", 40.0));
    }

    @Test
    void processClassPaymentConfiguredExistingPartnerTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": [42]}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": [1]}");
        HttpResponse createInvoiceResp = buildResponse("{\"result\": 100}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": [1]}");
        HttpResponse createPaymentResp = buildResponse("{\"result\": 200}");
        HttpResponse postPaymentResp   = buildResponse("{\"result\": true}");
        HttpResponse reconcileResp     = buildResponse("{\"result\": null}");

        doReturn(authResp)
                .doReturn(searchPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp).doReturn(createPaymentResp).doReturn(postPaymentResp)
                .doReturn(reconcileResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processClassPayment("student@test.com", "Student A",
                        "Prof. Ana", "Guitarra", "ONLINE", "Barcelona", 35.0));
    }

    // ── Configured — processClassRefund ──────────────────────────

    @Test
    void processClassRefundConfiguredFullFlowSuccessTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": [42]}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": [1]}");
        HttpResponse createInvoiceResp = buildResponse("{\"result\": 100}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": [1]}");
        HttpResponse createPaymentResp = buildResponse("{\"result\": 200}");
        HttpResponse postPaymentResp   = buildResponse("{\"result\": true}");
        HttpResponse reconcileResp     = buildResponse("{\"result\": null}");

        doReturn(authResp)
                .doReturn(searchPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp).doReturn(createPaymentResp).doReturn(postPaymentResp)
                .doReturn(reconcileResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processClassRefund("student@test.com", "Student A", "Prof. Ana", 40.0));
    }

    @Test
    void processClassRefundConfiguredHttpExceptionLogsErrorTest() throws Exception {
        configureOdoo();
        doThrow(new java.io.IOException("Connection refused"))
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processClassRefund("student@test.com", "Student A", "Prof. Ana", 40.0));
    }

    @Test
    void processClassRefundConfiguredPartnerNotFoundCreatesNewTest() throws Exception {
        configureOdoo();
        HttpResponse authResp          = buildResponse("{\"result\": 1}");
        HttpResponse searchPartnerResp = buildResponse("{\"result\": []}");
        HttpResponse createPartnerResp = buildResponse("{\"result\": 77}");
        HttpResponse searchCurrResp    = buildResponse("{\"result\": [1]}");
        HttpResponse createInvoiceResp = buildResponse("{\"result\": 150}");
        HttpResponse postInvoiceResp   = buildResponse("{\"result\": true}");
        HttpResponse searchJournalResp = buildResponse("{\"result\": [1]}");
        HttpResponse createPaymentResp = buildResponse("{\"result\": 250}");
        HttpResponse postPaymentResp   = buildResponse("{\"result\": true}");
        HttpResponse reconcileResp     = buildResponse("{\"result\": null}");

        doReturn(authResp)
                .doReturn(searchPartnerResp).doReturn(createPartnerResp)
                .doReturn(searchCurrResp).doReturn(createInvoiceResp).doReturn(postInvoiceResp)
                .doReturn(searchJournalResp).doReturn(createPaymentResp).doReturn(postPaymentResp)
                .doReturn(reconcileResp)
                .when(mockHttpClient).send(any(HttpRequest.class), any());

        assertDoesNotThrow(() ->
                odooService.processClassRefund("student@test.com", "Student B", "Prof. Juan", 35.0));
    }
}
