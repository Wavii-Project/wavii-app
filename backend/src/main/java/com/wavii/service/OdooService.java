package com.wavii.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de integración con Odoo ERP mediante JSON-RPC.
 * Gestiona la sincronización de clientes, facturación y tareas de proyectos.
 * 
 * @author eduglezexp
 */
@Service
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class OdooService {
    private static final String SUBSCRIPTION_PROJECT = "Suscripciones Wavii";
    private static final String VERIFICATION_MODEL = "wavii.teacher.verification";

    @Value("${odoo.url:}")
    private String odooUrl;

    @Value("${odoo.db:}")
    private String odooDb;

    @Value("${odoo.user:}")
    private String odooUser;

    @Value("${odoo.password:}")
    private String odooPassword;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Comprueba si el servicio de Odoo está configurado correctamente con los parámetros del entorno.
     * 
     * @return true si la configuración mínima está presente.
     */
    public boolean isConfigured() {
        return odooUrl != null && !odooUrl.isBlank()
                && odooDb != null && !odooDb.isBlank()
                && odooUser != null && !odooUser.isBlank();
    }


    // ── Capa de transporte JSON-RPC ──

    /** Llamada genérica al endpoint /jsonrpc de Odoo. Usa raw List para datos heterogéneos. */
    private Object callRpc(String service, String method, List args) throws Exception {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "method", "call",
                "id", 1,
                "params", Map.of(
                        "service", service,
                        "method", method,
                        "args", args
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(odooUrl + "/jsonrpc"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> json = mapper.readValue(response.body(), Map.class);

        if (json.containsKey("error")) {
            throw new RuntimeException("Odoo RPC error: " + json.get("error"));
        }
        return json.get("result");
    }

    /**
     * Llama a execute_kw para operar sobre un modelo de Odoo.
     * args es la lista de argumentos posicionales del método del modelo.
     */
    private Object executeKw(int uid, String model, String method,
                              List args, Map kwargs) throws Exception {
        List rpcArgs = new ArrayList();
        rpcArgs.add(odooDb);
        rpcArgs.add(uid);
        rpcArgs.add(odooPassword);
        rpcArgs.add(model);
        rpcArgs.add(method);
        rpcArgs.add(args);
        rpcArgs.add(kwargs);
        return callRpc("object", "execute_kw", rpcArgs);
    }

    private Object executeKw(int uid, String model, String method, List args) throws Exception {
        return executeKw(uid, model, method, args, Map.of());
    }

    // ── Helpers de modelo Odoo ──

    /** Busca registros por dominio y devuelve sus IDs. */
    private List<Integer> search(int uid, String model, List domain) throws Exception {
        List args = new ArrayList();
        args.add(domain);
        Object result = executeKw(uid, model, "search", args);
        List<?> raw = (List<?>) result;
        return raw.stream().map(o -> ((Number) o).intValue()).toList();
    }

    /** Crea un registro y devuelve su ID. Odoo 17 puede devolver [id] o id. */
    private int create(int uid, String model, Map<String, Object> values) throws Exception {
        List createArgs = new ArrayList();
        List valsList = new ArrayList();
        valsList.add(values);
        createArgs.add(valsList);
        Object result = executeKw(uid, model, "create", createArgs);
        if (result instanceof List<?> list) {
            return ((Number) list.get(0)).intValue();
        }
        return ((Number) result).intValue();
    }

    // ── Autenticación ──

    private int authenticate() throws Exception {
        List args = new ArrayList();
        args.add(odooDb);
        args.add(odooUser);
        args.add(odooPassword);
        args.add(Map.of());
        Object result = callRpc("common", "authenticate", args);
        if (result == null || Boolean.FALSE.equals(result)) {
            throw new RuntimeException("Autenticacion Odoo fallida — verifica credenciales en .env");
        }
        return ((Number) result).intValue();
    }

    // ── Verificaciones ──

    private static final String VERIFICATION_PROJECT = "Verificaciones Wavii";

    /**
     * Busca un partner (cliente) en Odoo por su email o lo crea si no existe.
     * 
     * @param email Email del cliente.
     * @param name Nombre del cliente.
     * @param uid ID de usuario autenticado en Odoo.
     * @return ID del partner en Odoo.
     * @throws Exception Si falla la comunicación con Odoo.
     */
    public int findOrCreatePartner(String email, String name, int uid) throws Exception {
        List condition = Arrays.asList("email", "=", email);
        List domain = new ArrayList();
        domain.add(condition);

        List<Integer> existing = search(uid, "res.partner", domain);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Map<String, Object> vals = new LinkedHashMap<>();
        vals.put("name", (name != null && !name.isBlank()) ? name : email);
        vals.put("email", email);
        vals.put("customer_rank", 1);
        int partnerId = create(uid, "res.partner", vals);
        log.debug("Odoo: nuevo partner creado id={} email={}", partnerId, email);
        return partnerId;
    }

    /**
     * Crea una factura de cliente en Odoo y la confirma automáticamente.
     * 
     * @param partnerId ID del cliente.
     * @param planName Nombre del concepto o plan.
     * @param amount Importe de la factura.
     * @param currency Moneda (ej. "EUR").
     * @param uid ID de usuario de Odoo.
     * @return ID de la factura creada.
     * @throws Exception Si falla la comunicación con Odoo.
     */
    public int createInvoice(int partnerId, String planName, double amount,
                              String currency, int uid) throws Exception {
        // Buscar el ID de la moneda
        List currencyCondition = Arrays.asList("name", "=", currency.toUpperCase());
        List currencyDomain = new ArrayList();
        currencyDomain.add(currencyCondition);
        List<Integer> currencies = search(uid, "res.currency", currencyDomain);
        int currencyId = currencies.isEmpty() ? 1 : currencies.get(0);

        // Línea de factura como comando ORM: (0, 0, {vals})
        Map<String, Object> invoiceLine = new LinkedHashMap<>();
        invoiceLine.put("name", "Suscripcion Wavii — " + planName);
        invoiceLine.put("quantity", 1.0);
        invoiceLine.put("price_unit", amount);
        List lineCmd = Arrays.asList(0, 0, invoiceLine);

        List invoiceLines = new ArrayList();
        invoiceLines.add(lineCmd);

        Map<String, Object> invoiceVals = new LinkedHashMap<>();
        invoiceVals.put("move_type", "out_invoice");
        invoiceVals.put("partner_id", partnerId);
        invoiceVals.put("currency_id", currencyId);
        invoiceVals.put("invoice_line_ids", invoiceLines);

        int invoiceId = create(uid, "account.move", invoiceVals);

        // Confirmar la factura: draft → posted
        List invoiceIds = new ArrayList();
        invoiceIds.add(invoiceId);
        List postArgs = new ArrayList();
        postArgs.add(invoiceIds);
        executeKw(uid, "account.move", "action_post", postArgs);

        log.debug("Odoo: factura creada y confirmada id={}", invoiceId);
        return invoiceId;
    }

    /**
     * Registra un pago para una factura existente en Odoo mediante el asistente de pago.
     * 
     * @param invoiceId ID de la factura.
     * @param partnerId ID del cliente.
     * @param amount Importe pagado.
     * @param uid ID de usuario de Odoo.
     * @throws Exception Si falla la comunicación con Odoo.
     */
    public void registerPayment(int invoiceId, int partnerId, double amount, int uid) throws Exception {
        List journalCondition = Arrays.asList("type", "in", Arrays.asList("bank", "cash"));
        List journalDomain = new ArrayList();
        journalDomain.add(journalCondition);
        List<Integer> journals = search(uid, "account.journal", journalDomain);

        if (journals.isEmpty()) {
            log.warn("Odoo: no se encontro un journal de banco/caja - pago no registrado");
            return;
        }
        int journalId = journals.get(0);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("active_model", "account.move");
        context.put("active_ids", List.of(invoiceId));
        context.put("active_id", invoiceId);

        Map<String, Object> registerVals = new LinkedHashMap<>();
        registerVals.put("amount", amount);
        registerVals.put("journal_id", journalId);
        registerVals.put("partner_id", partnerId);

        List createArgs = new ArrayList();
        createArgs.add(registerVals);

        Object wizardResult = executeKw(
                uid,
                "account.payment.register",
                "create",
                createArgs,
                Map.of("context", context)
        );

        int wizardId;
        if (wizardResult instanceof List<?> list) {
            wizardId = ((Number) list.get(0)).intValue();
        } else {
            wizardId = ((Number) wizardResult).intValue();
        }

        List actionArgs = new ArrayList();
        actionArgs.add(List.of(wizardId));
        executeKw(
                uid,
                "account.payment.register",
                "action_create_payments",
                actionArgs,
                Map.of("context", context)
        );

        log.debug("Odoo: pago registrado y conciliado para factura id={} mediante wizard id={}", invoiceId, wizardId);
    }

    /**
     * Crea un contacto en el CRM de Odoo de forma asíncrona tras la verificación de email.
     * 
     * @param name Nombre del contacto.
     * @param email Email del contacto.
     * @param role Rol asignado en Wavii.
     * @param subscription Plan de suscripción actual.
     */
    @Async
    public void createCrmContact(String name, String email, String role, String subscription) {
        if (!isConfigured()) {
            log.info("[DEV] Odoo no configurado — contacto CRM no creado para {}", email);
            return;
        }
        try {
            int uid = authenticate();

            List condition = Arrays.asList("email", "=", email);
            List domain = new ArrayList();
            domain.add(condition);
            List<Integer> existing = search(uid, "res.partner", domain);

            if (!existing.isEmpty()) {
                log.debug("Odoo CRM: partner ya existe id={} email={}", existing.get(0), email);
                return;
            }

            Map<String, Object> vals = new LinkedHashMap<>();
            vals.put("name", (name != null && !name.isBlank()) ? name : email);
            vals.put("email", email);
            vals.put("comment", "Rol: " + role + " | Plan: " + subscription);
            vals.put("customer_rank", 1);
            int partnerId = create(uid, "res.partner", vals);
            log.info("Odoo CRM: contacto creado id={} email={}", partnerId, email);
        } catch (Exception e) {
            log.warn("Odoo CRM: error creando contacto para {} — {}", email, e.getMessage());
        }
    }

    /**
     * Crea una tarea de verificación en el proyecto correspondiente de Odoo.
     * 
     * @param userName Nombre del usuario profesor.
     * @param email Email del usuario.
     * @param fileName Nombre del archivo subido.
     * @param documentUrl URL para visualizar el documento.
     */
    @Async
    public void createVerificationTask(
            String userId,
            String userName,
            String email,
            String fileName,
            String mimeType,
            byte[] fileBytes,
            String documentUrl
    ) {
        if (!isConfigured()) {
            log.info("[DEV] Odoo no configurado — tarea de verificación no creada para {}", email);
            return;
        }
        try {
            int uid = authenticate();
            if (createVerificationRecord(uid, userId, userName, email, fileName, mimeType, fileBytes)) {
                log.info("Odoo: verificacion creada en modulo personalizado para {}", email);
                return;
            }

            List projCondition = Arrays.asList("name", "=", VERIFICATION_PROJECT);
            List projDomain = new ArrayList();
            projDomain.add(projCondition);
            List<Integer> projects = search(uid, "project.project", projDomain);

            int projectId;
            if (projects.isEmpty()) {
                Map<String, Object> projVals = new LinkedHashMap<>();
                projVals.put("name", VERIFICATION_PROJECT);
                projectId = create(uid, "project.project", projVals);
                log.debug("Odoo: proyecto '{}' creado id={}", VERIFICATION_PROJECT, projectId);
            } else {
                projectId = projects.get(0);
            }

            String description = "UserId: " + userId
                    + "\nEmail: " + email
                    + "\nDocumento: " + fileName
                    + (documentUrl != null ? "\nURL: " + documentUrl : "");

            Map<String, Object> taskVals = new LinkedHashMap<>();
            taskVals.put("name", "Verificar profesor: " + userName);
            taskVals.put("description", description);
            taskVals.put("project_id", projectId);
            int taskId = create(uid, "project.task", taskVals);
            if (fileBytes != null && fileBytes.length > 0) {
                createVerificationAttachment(uid, taskId, "project.task", fileName, mimeType, fileBytes);
            }
            log.info("Odoo: tarea de verificación creada id={} para {}", taskId, email);
        } catch (Exception e) {
            log.warn("Odoo: error creando tarea de verificación para {} — {}", email, e.getMessage());
        }
    }

    /**
     * Crea una tarea de gestión de suscripción en el proyecto correspondiente de Odoo.
     * 
     * @param userName Nombre del usuario.
     * @param email Email del usuario.
     * @param title Título de la tarea.
     * @param description Descripción detallada.
     */
    private boolean createVerificationRecord(
            int uid,
            String userId,
            String userName,
            String email,
            String fileName,
            String mimeType,
            byte[] fileBytes
    ) {
        try {
            Map<String, Object> vals = new LinkedHashMap<>();
            vals.put("user_id", userId);
            vals.put("teacher_name", userName);
            vals.put("email", email);
            vals.put("document_filename", fileName);
            vals.put("status", "pending");
            int verificationId = create(uid, VERIFICATION_MODEL, vals);
            if (fileBytes != null && fileBytes.length > 0) {
                int attachmentId = createVerificationAttachment(uid, verificationId, VERIFICATION_MODEL, fileName, mimeType, fileBytes);
                List writeArgs = new ArrayList();
                writeArgs.add(List.of(verificationId));
                writeArgs.add(Map.of("document_attachment_id", attachmentId));
                executeKw(uid, VERIFICATION_MODEL, "write", writeArgs);
            }
            return true;
        } catch (Exception e) {
            log.info("Odoo: modulo {} no disponible, fallback a project.task: {}", VERIFICATION_MODEL, e.getMessage());
            return false;
        }
    }

    private int createVerificationAttachment(int uid, int recordId, String resModel, String fileName, String mimeType, byte[] fileBytes) throws Exception {
        Map<String, Object> attachmentVals = new LinkedHashMap<>();
        attachmentVals.put("name", fileName);
        attachmentVals.put("type", "binary");
        attachmentVals.put("res_model", resModel);
        attachmentVals.put("res_id", recordId);
        attachmentVals.put("mimetype", (mimeType != null && !mimeType.isBlank()) ? mimeType : "application/pdf");
        attachmentVals.put("datas", Base64.getEncoder().encodeToString(fileBytes));
        return create(uid, "ir.attachment", attachmentVals);
    }

    @Async
    public void createSubscriptionTask(String userName, String email, String title, String description) {
        if (!isConfigured()) {
            log.info("[DEV] Odoo no configurado - tarea de suscripción no creada para {}", email);
            return;
        }
        try {
            int uid = authenticate();

            List projCondition = Arrays.asList("name", "=", SUBSCRIPTION_PROJECT);
            List projDomain = new ArrayList();
            projDomain.add(projCondition);
            List<Integer> projects = search(uid, "project.project", projDomain);

            int projectId;
            if (projects.isEmpty()) {
                Map<String, Object> projVals = new LinkedHashMap<>();
                projVals.put("name", SUBSCRIPTION_PROJECT);
                projectId = create(uid, "project.project", projVals);
            } else {
                projectId = projects.get(0);
            }

            Map<String, Object> taskVals = new LinkedHashMap<>();
            taskVals.put("name", title);
            taskVals.put("description", "Usuario: " + userName + "\nEmail: " + email + "\n\n" + description);
            taskVals.put("project_id", projectId);
            create(uid, "project.task", taskVals);
        } catch (Exception e) {
            log.warn("Odoo: error creando tarea de suscripción para {} - {}", email, e.getMessage());
        }
    }

    /**
     * Procesa un pago de Stripe de forma asíncrona: crea partner, factura y registra el pago en Odoo.
     * 
     * @param customerEmail Email del cliente.
     * @param customerName Nombre del cliente.
     * @param planName Nombre del plan suscrito.
     * @param amount Importe pagado.
     */
    @Async
    public void processStripePayment(String customerEmail, String customerName,
                                     String planName, double amount) {
        if (!isConfigured()) {
            log.info("[DEV] Odoo no configurado — factura no generada para {}", customerEmail);
            return;
        }
        try {
            int uid = authenticate();
            int partnerId = findOrCreatePartner(customerEmail, customerName, uid);
            int invoiceId = createInvoice(partnerId, planName, amount, "EUR", uid);
            registerPayment(invoiceId, partnerId, amount, uid);
            log.info("Odoo: factura generada correctamente para {} — plan={} importe={}EUR",
                    customerEmail, planName, amount);
        } catch (Exception e) {
            log.error("Odoo: error procesando pago de {} ({}): {}", customerEmail, planName, e.getMessage());
        }
    }

    /**
     * Procesa el pago de una clase particular en Odoo.
     * 
     * @param studentEmail Email del alumno.
     * @param studentName Nombre del alumno.
     * @param teacherName Nombre del profesor.
     * @param instrument Instrumento de la clase.
     * @param modality Modalidad de la clase.
     * @param city Ciudad de la clase.
     * @param amount Importe pagado.
     */
    @Async
    public void processClassPayment(String studentEmail, String studentName,
                                    String teacherName, String instrument,
                                    String modality, String city, double amount) {
        if (!isConfigured()) {
            log.info("[DEV] Odoo no configurado — factura de clase no generada para {}", studentEmail);
            return;
        }
        try {
            int uid = authenticate();
            String partnerLabel = studentName + " / " + teacherName;
            int partnerId = findOrCreatePartner(studentEmail, partnerLabel, uid);
            String planName = "Clase con " + teacherName;
            int invoiceId = createInvoice(partnerId, planName, amount, "EUR", uid);
            registerPayment(invoiceId, partnerId, amount, uid);
            log.info("Odoo: factura de clase generada para {} con {} por {}EUR",
                    studentEmail, teacherName, amount);
        } catch (Exception e) {
            log.error("Odoo: error procesando pago de clase para {}: {}", studentEmail, e.getMessage());
        }
    }

    /**
     * Procesa el reembolso de una clase particular en Odoo.
     * 
     * @param studentEmail Email del alumno.
     * @param studentName Nombre del alumno.
     * @param teacherName Nombre del profesor.
     * @param amount Importe reembolsado.
     */
    @Async
    public void processClassRefund(String studentEmail, String studentName,
                                   String teacherName, double amount) {
        if (!isConfigured()) {
            log.info("[DEV] Odoo no configurado â€” devoluciÃ³n de clase no generada para {}", studentEmail);
            return;
        }
        try {
            int uid = authenticate();
            String partnerLabel = studentName + " / " + teacherName;
            int partnerId = findOrCreatePartner(studentEmail, partnerLabel, uid);
            int invoiceId = createInvoice(partnerId, "Reembolso clase con " + teacherName, -Math.abs(amount), "EUR", uid);
            registerPayment(invoiceId, partnerId, -Math.abs(amount), uid);
            log.info("Odoo: reembolso de clase registrado para {} con {} por {}EUR",
                    studentEmail, teacherName, amount);
        } catch (Exception e) {
            log.error("Odoo: error procesando reembolso de clase para {}: {}", studentEmail, e.getMessage());
        }
    }
}

