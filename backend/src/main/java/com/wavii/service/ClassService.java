package com.wavii.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.wavii.model.ClassEnrollment;
import com.wavii.model.ClassMessage;
import com.wavii.model.ClassPost;
import com.wavii.model.ClassSession;
import com.wavii.model.TeacherReport;
import com.wavii.model.User;
import com.wavii.model.enums.Role;
import com.wavii.repository.ClassEnrollmentRepository;
import com.wavii.repository.ClassMessageRepository;
import com.wavii.repository.ClassPostRepository;
import com.wavii.repository.ClassSessionRepository;
import com.wavii.repository.TeacherReportRepository;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_SCHEDULED = "scheduled";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_REFUND_REQUESTED = "refund_requested";
    private static final String STATUS_REFUNDED = "refunded";
    private static final String STATUS_CANCELLED = "cancelled";

    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassMessageRepository messageRepository;
    private final ClassPostRepository postRepository;
    private final ClassSessionRepository sessionRepository;
    private final TeacherReportRepository teacherReportRepository;
    private final StripeService stripeService;
    private final OdooService odooService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listClasses(User currentUser) {
        List<ClassEnrollment> enrollments = enrollmentRepository.findByStudentOrderByCreatedAtDesc(currentUser);

        return enrollments.stream()
                .map(this::toEnrollmentMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getManageOverview(User currentUser) {
        if (!isTeacher(currentUser)) {
            throw new IllegalArgumentException("Solo un profesor puede gestionar sus clases");
        }

        List<Map<String, Object>> classes = enrollmentRepository.findByTeacherOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::toEnrollmentMap)
                .toList();
        List<Map<String, Object>> sessions = sessionRepository.findByTeacherOrderByScheduledAtAsc(currentUser)
                .stream()
                .map(this::toSessionMap)
                .toList();
        List<Map<String, Object>> posts = postRepository.findByTeacherOrderByCreatedAtDesc(currentUser).stream()
                .map(this::toPostMap)
                .toList();

        return Map.of(
                "classes", classes,
                "sessions", sessions,
                "posts", posts
        );
    }

    @Transactional
    public Map<String, Object> checkout(UUID teacherId, User student) throws Exception {
        if (student == null) {
            throw new IllegalArgumentException("Sesion no valida");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        if (!isTeacher(teacher)) {
            throw new IllegalArgumentException("El usuario no es profesor");
        }
        if (teacher.getId().equals(student.getId())) {
            throw new IllegalArgumentException("No puedes suscribirte a tu propia clase");
        }
        if (teacher.getPricePerHour() == null) {
            throw new IllegalArgumentException("El profesor no tiene precio configurado");
        }

        ClassEnrollment enrollment = buildEnrollment(teacher, student);
        enrollment = enrollmentRepository.save(enrollment);

        if (teacher.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
            markPaidEnrollment(enrollment, "free_class_" + enrollment.getId(), "free_receipt_" + enrollment.getId());
            return checkoutResponse(enrollment, "free_class_no_payment", true);
        }

        if (!stripeService.isConfigured()) {
            markPaidEnrollment(enrollment, "dev_pi_" + enrollment.getId(), "dev_receipt_" + enrollment.getId());
            return checkoutResponse(enrollment, "dev_class_client_secret", true);
        }

        String customerId = stripeService.createOrGetCustomer(student);
        student.setStripeCustomerId(customerId);
        userRepository.save(student);

        long amountCents = teacher.getPricePerHour().movePointRight(2).longValueExact();
        Map<String, Object> intent = stripeService.createClassPaymentIntent(
                customerId,
                teacher.getId().toString(),
                teacher.getName(),
                student.getName(),
                teacher.getInstrument(),
                teacher.getClassModality(),
                teacher.getCity(),
                amountCents);

        enrollment.setStripePaymentIntentId((String) intent.get("paymentIntentId"));
        enrollmentRepository.save(enrollment);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enrollmentId", enrollment.getId().toString());
        response.put("paymentIntentId", intent.get("paymentIntentId"));
        response.put("clientSecret", intent.get("clientSecret"));
        response.put("devMode", false);
        return response;
    }

    @Transactional
    public Map<String, Object> requestClass(UUID teacherId, User student, Map<String, String> body) {
        if (student == null) {
            throw new IllegalArgumentException("Sesion no valida");
        }

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        if (!isTeacher(teacher)) {
            throw new IllegalArgumentException("El usuario no es profesor");
        }
        if (teacher.getId().equals(student.getId())) {
            throw new IllegalArgumentException("No puedes solicitar clases a tu propio perfil");
        }

        String message = normalize(body.get("message"));
        String availability = normalize(body.get("availability"));
        String requestedModality = normalize(body.get("requestedModality"));

        enrollmentRepository.findFirstByTeacherAndStudentOrderByCreatedAtDesc(teacher, student)
                .filter(existing -> isActiveRequestStatus(existing.getPaymentStatus()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya tienes una solicitud activa con este profesor");
                });

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .instrument(teacher.getInstrument())
                .city(teacher.getCity())
                .province(teacher.getProvince())
                .modality(teacher.getClassModality())
                .requestedModality(requestedModality != null ? requestedModality : teacher.getClassModality())
                .unitPrice(teacher.getPricePerHour())
                .paymentStatus(STATUS_PENDING)
                .requestMessage(message)
                .requestAvailability(availability)
                .hoursPurchased(1)
                .hoursUsed(0)
                .build();

        enrollment = enrollmentRepository.save(enrollment);
        notifyRequestCreated(enrollment);
        return toEnrollmentMap(enrollment);
    }

    @Transactional
    public Map<String, Object> confirm(UUID enrollmentId, User currentUser) {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        ensureParticipant(enrollment, currentUser);

        if (isPaidState(enrollment.getPaymentStatus())) {
            return toEnrollmentMap(enrollment);
        }
        if (!STATUS_PENDING.equalsIgnoreCase(nullSafeStatus(enrollment))) {
            throw new IllegalArgumentException("Esta clase no se puede confirmar en su estado actual");
        }

        if (!stripeService.isConfigured()) {
            markPaidEnrollment(enrollment,
                    blankToDefault(enrollment.getStripePaymentIntentId(), "manual_" + enrollment.getId()),
                    "receipt_" + enrollment.getId());
            return toEnrollmentMap(enrollment);
        }

        String paymentIntentId = enrollment.getStripePaymentIntentId();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("No hay un pago pendiente para esta clase");
        }

        try {
            PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(paymentIntentId);
            if (!"succeeded".equalsIgnoreCase(paymentIntent.getStatus())) {
                throw new IllegalArgumentException("El pago no se ha completado todavia");
            }
            markPaidEnrollment(enrollment, paymentIntentId, paymentIntent.getLatestCharge());
            return toEnrollmentMap(enrollment);
        } catch (StripeException e) {
            log.warn("Error confirmando pago Stripe de clase {}: {}", enrollmentId, e.getMessage());
            throw new IllegalArgumentException("No se pudo validar el pago con Stripe");
        }
    }

    @Transactional
    public Map<String, Object> updateStatus(UUID enrollmentId, User currentUser, Map<String, String> body) {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        ensureParticipant(enrollment, currentUser);

        String nextStatus = normalize(body.get("status"));
        String reason = normalize(body.get("reason"));
        if (nextStatus == null) {
            throw new IllegalArgumentException("Debes indicar un estado");
        }

        if (!isAllowedStatus(nextStatus)) {
            throw new IllegalArgumentException("Estado no valido");
        }

        boolean teacherAction = enrollment.getTeacher().getId().equals(currentUser.getId());
        if (!teacherAction && !STATUS_CANCELLED.equalsIgnoreCase(nextStatus)) {
            throw new IllegalArgumentException("Solo el profesor puede aceptar o completar una solicitud");
        }
        if (STATUS_ACCEPTED.equalsIgnoreCase(nextStatus) && !teacherAction) {
            throw new IllegalArgumentException("Solo el profesor puede aceptar una solicitud");
        }
        if ((STATUS_REJECTED.equalsIgnoreCase(nextStatus) || STATUS_COMPLETED.equalsIgnoreCase(nextStatus))
                && !teacherAction) {
            throw new IllegalArgumentException("Solo el profesor puede modificar este estado");
        }
        if (STATUS_PENDING.equalsIgnoreCase(nullSafeStatus(enrollment)) && STATUS_COMPLETED.equalsIgnoreCase(nextStatus)) {
            throw new IllegalArgumentException("Primero debes aceptar la solicitud");
        }

        enrollment.setPaymentStatus(nextStatus.toLowerCase());
        enrollmentRepository.save(enrollment);
        notifyStatusChange(enrollment, nextStatus.toLowerCase(), currentUser, reason);
        return toEnrollmentMap(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMessages(UUID enrollmentId, User currentUser) {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        ensureParticipant(enrollment, currentUser);
        ensureChatEnabled(enrollment);
        return messageRepository.findByEnrollmentOrderByCreatedAtAsc(enrollment)
                .stream()
                .map(this::toMessageMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> sendMessage(UUID enrollmentId, User sender, String content) {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        ensureParticipant(enrollment, sender);
        ensureChatEnabled(enrollment);

        String cleaned = content == null ? "" : content.trim();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacio");
        }

        ClassMessage message = ClassMessage.builder()
                .enrollment(enrollment)
                .sender(sender)
                .content(cleaned)
                .build();
        return toMessageMap(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPosts(User teacher) {
        if (!isTeacher(teacher)) {
            return List.of();
        }
        return postRepository.findByTeacherOrderByCreatedAtDesc(teacher).stream()
                .map(this::toPostMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostsForViewer(User viewer, UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        if (!isTeacher(teacher)) {
            return List.of();
        }
        if (viewer == null) {
            throw new IllegalArgumentException("Sesion no valida");
        }
        boolean canView = viewer.getId().equals(teacherId)
                || enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, viewer, STATUS_ACCEPTED)
                || enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, viewer, STATUS_PAID)
                || enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, viewer, STATUS_SCHEDULED)
                || enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, viewer, STATUS_COMPLETED);
        if (!canView) {
            throw new IllegalArgumentException("No tienes acceso a estas noticias");
        }
        return postRepository.findByTeacherOrderByCreatedAtDesc(teacher).stream()
                .map(this::toPostMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostsForStudent(User student) {
        if (student == null) {
            throw new IllegalArgumentException("Sesion no valida");
        }

        Map<UUID, User> activeTeachers = new LinkedHashMap<>();
        enrollmentRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .filter(enrollment -> {
                    String status = nullSafeStatus(enrollment);
                    return STATUS_ACCEPTED.equalsIgnoreCase(status)
                            || STATUS_PAID.equalsIgnoreCase(status)
                            || STATUS_SCHEDULED.equalsIgnoreCase(status)
                            || STATUS_COMPLETED.equalsIgnoreCase(status);
                })
                .forEach(enrollment -> {
                    if (enrollment.getTeacher() != null) {
                        activeTeachers.putIfAbsent(enrollment.getTeacher().getId(), enrollment.getTeacher());
                    }
                });

        return activeTeachers.values().stream()
                .flatMap(teacher -> postRepository.findByTeacherOrderByCreatedAtDesc(teacher).stream())
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::toPostMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> createPost(User teacher, String title, String content) {
        if (!isTeacher(teacher)) {
            throw new IllegalArgumentException("Solo un profesor puede publicar noticias");
        }
        String cleanTitle = title == null ? "" : title.trim();
        String cleanContent = content == null ? "" : content.trim();
        if (cleanTitle.isBlank() || cleanContent.isBlank()) {
            throw new IllegalArgumentException("El titulo y el contenido son obligatorios");
        }
        ClassPost post = ClassPost.builder()
                .teacher(teacher)
                .title(cleanTitle)
                .content(cleanContent)
                .build();
        ClassPost saved = postRepository.save(post);
        notifyStudentsAboutPost(saved);
        return toPostMap(saved);
    }

    @Transactional
    public Map<String, Object> requestExtraHour(UUID enrollmentId, User currentUser) throws Exception {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        ensureStudent(enrollment, currentUser);
        return checkout(enrollment.getTeacher().getId(), currentUser);
    }

    @Transactional
    public Map<String, Object> requestRefund(UUID enrollmentId, User currentUser) {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        ensureStudent(enrollment, currentUser);
        if (!isPaidState(enrollment.getPaymentStatus())) {
            throw new IllegalArgumentException("Solo puedes pedir reembolso de una clase pagada");
        }
        if (STATUS_COMPLETED.equalsIgnoreCase(nullSafeStatus(enrollment))) {
            throw new IllegalArgumentException("No puedes pedir reembolso de una clase ya completada");
        }

        String paymentIntentId = enrollment.getStripePaymentIntentId();
        if (stripeService.isConfigured() && paymentIntentId != null && !paymentIntentId.startsWith("free_class_")) {
            try {
                stripeService.refundPaymentIntent(paymentIntentId);
            } catch (StripeException e) {
                log.warn("Error solicitando refund Stripe para {}: {}", enrollmentId, e.getMessage());
                throw new IllegalArgumentException("No se pudo solicitar el reembolso");
            }
        }

        enrollment.setPaymentStatus(STATUS_REFUNDED);
        enrollmentRepository.save(enrollment);

        BigDecimal amount = enrollment.getUnitPrice() != null ? enrollment.getUnitPrice() : BigDecimal.ZERO;
        odooService.processClassRefund(
                enrollment.getStudent().getEmail(),
                enrollment.getStudent().getName(),
                enrollment.getTeacher().getName(),
                amount.doubleValue());

        notificationService.create(
                enrollment.getTeacher(),
                "class_refund",
                "Se ha reembolsado una clase",
                enrollment.getStudent().getName() + " ha solicitado y recibido el reembolso de una hora.",
                Map.of("enrollmentId", enrollment.getId().toString()));

        return toEnrollmentMap(enrollment);
    }

    @Transactional
    public Map<String, Object> createSession(UUID enrollmentId, User currentUser, Map<String, String> body) {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        ensureTeacherOwner(enrollment.getTeacher(), currentUser);
        if (!isSchedulableState(enrollment.getPaymentStatus())) {
            throw new IllegalArgumentException("La solicitud debe estar aceptada antes de agendarla");
        }

        String scheduledAtRaw = body.get("scheduledAt");
        if (scheduledAtRaw == null || scheduledAtRaw.isBlank()) {
            throw new IllegalArgumentException("La fecha y hora son obligatorias");
        }

        LocalDateTime scheduledAt = LocalDateTime.parse(scheduledAtRaw);
        Integer duration = parseDuration(body.get("durationMinutes"));
        String meetingUrl = normalize(body.get("meetingUrl"));
        String notes = normalize(body.get("notes"));

        ClassSession session = ClassSession.builder()
                .enrollment(enrollment)
                .teacher(enrollment.getTeacher())
                .student(enrollment.getStudent())
                .scheduledAt(scheduledAt)
                .durationMinutes(duration)
                .meetingUrl(meetingUrl)
                .notes(notes)
                .status(STATUS_SCHEDULED)
                .build();
        session = sessionRepository.save(session);

        enrollment.setPaymentStatus(STATUS_SCHEDULED);
        enrollment.setClassLink(meetingUrl);
        enrollmentRepository.save(enrollment);

        notifyScheduling(enrollment, session);
        return toSessionMap(session);
    }

    @Transactional
    public Map<String, Object> updateSession(UUID sessionId, User currentUser, Map<String, String> body) {
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesion no encontrada"));
        ensureTeacherOwner(session.getTeacher(), currentUser);

        String nextStatus = normalize(body.get("status"));
        if (body.containsKey("scheduledAt") && body.get("scheduledAt") != null && !body.get("scheduledAt").isBlank()) {
            session.setScheduledAt(LocalDateTime.parse(body.get("scheduledAt")));
        }
        if (body.containsKey("durationMinutes")) {
            session.setDurationMinutes(parseDuration(body.get("durationMinutes")));
        }
        if (body.containsKey("meetingUrl")) {
            session.setMeetingUrl(normalize(body.get("meetingUrl")));
        }
        if (body.containsKey("notes")) {
            session.setNotes(normalize(body.get("notes")));
        }
        if (nextStatus != null) {
            session.setStatus(nextStatus.toLowerCase());
        }

        session = sessionRepository.save(session);
        syncEnrollmentWithSessions(session.getEnrollment(), nextStatus);
        notifySessionUpdate(session);
        return toSessionMap(session);
    }

    @Transactional
    public Map<String, Object> createTeacherReport(UUID teacherId, User reporter, Map<String, String> body) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));
        if (!isTeacher(teacher)) {
            throw new IllegalArgumentException("Solo puedes reportar a un profesor");
        }
        if (reporter == null) {
            throw new IllegalArgumentException("Sesion no valida");
        }
        if (teacher.getId().equals(reporter.getId())) {
            throw new IllegalArgumentException("No puedes reportarte a ti mismo");
        }

        String reason = normalize(body.get("reason"));
        if (reason == null) {
            throw new IllegalArgumentException("Debes indicar un motivo");
        }

        TeacherReport report = TeacherReport.builder()
                .teacher(teacher)
                .reporter(reporter)
                .reason(reason)
                .details(normalize(body.get("details")))
                .build();
        report = teacherReportRepository.save(report);
        return Map.of(
                "id", report.getId().toString(),
                "status", report.getStatus()
        );
    }

    public void ensureTeacherOwner(User teacher, User currentUser) {
        if (currentUser == null || !teacher.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("No tienes permiso para gestionar esta clase");
        }
    }

    private boolean isTeacher(User user) {
        return user != null && (user.getRole() == Role.PROFESOR_PARTICULAR || user.getRole() == Role.PROFESOR_CERTIFICADO);
    }

    private boolean isStudentVisible(ClassEnrollment enrollment) {
        return true;
    }

    private boolean isPaidState(String status) {
        String normalized = nullSafeStatus(status);
        return STATUS_PAID.equalsIgnoreCase(normalized)
                || STATUS_ACCEPTED.equalsIgnoreCase(normalized)
                || STATUS_SCHEDULED.equalsIgnoreCase(normalized)
                || STATUS_COMPLETED.equalsIgnoreCase(normalized)
                || STATUS_REFUND_REQUESTED.equalsIgnoreCase(normalized);
    }

    private void ensureChatEnabled(ClassEnrollment enrollment) {
        if (!isChatOpenState(enrollment.getPaymentStatus())) {
            throw new IllegalArgumentException("La solicitud todavia no ha sido aceptada");
        }
        if (STATUS_REFUNDED.equalsIgnoreCase(nullSafeStatus(enrollment))) {
            throw new IllegalArgumentException("Esta clase ya ha sido reembolsada");
        }
    }

    private void ensureParticipant(ClassEnrollment enrollment, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Sesion no valida");
        }
        boolean allowed = enrollment.getTeacher().getId().equals(currentUser.getId())
                || enrollment.getStudent().getId().equals(currentUser.getId());
        if (!allowed) {
            throw new IllegalArgumentException("No tienes acceso a esta clase");
        }
    }

    private void ensureStudent(ClassEnrollment enrollment, User currentUser) {
        if (currentUser == null || !enrollment.getStudent().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Solo el alumno puede realizar esta accion");
        }
    }

    private ClassEnrollment getEnrollment(UUID enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada"));
    }

    private ClassEnrollment buildEnrollment(User teacher, User student) {
        return ClassEnrollment.builder()
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .instrument(teacher.getInstrument())
                .city(teacher.getCity())
                .province(teacher.getProvince())
                .modality(teacher.getClassModality())
                .unitPrice(teacher.getPricePerHour())
                .paymentStatus(STATUS_PENDING)
                .hoursPurchased(1)
                .hoursUsed(0)
                .build();
    }

    private void markPaidEnrollment(ClassEnrollment enrollment, String paymentIntentId, String receiptNumber) {
        enrollment.setPaymentStatus(STATUS_PAID);
        enrollment.setStripePaymentIntentId(paymentIntentId);
        enrollment.setPaymentReceiptNumber(receiptNumber);
        enrollmentRepository.save(enrollment);
        odooService.processClassPayment(
                enrollment.getStudent().getEmail(),
                enrollment.getStudent().getName(),
                enrollment.getTeacher().getName(),
                enrollment.getInstrument(),
                enrollment.getModality(),
                enrollment.getCity(),
                enrollment.getUnitPrice() != null ? enrollment.getUnitPrice().doubleValue() : 0.0);
    }

    private Map<String, Object> checkoutResponse(ClassEnrollment enrollment, String clientSecret, boolean devMode) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enrollmentId", enrollment.getId().toString());
        response.put("paymentIntentId", enrollment.getStripePaymentIntentId());
        response.put("clientSecret", clientSecret);
        response.put("devMode", devMode);
        return response;
    }

    private void syncEnrollmentWithSessions(ClassEnrollment enrollment, String requestedStatus) {
        List<ClassSession> sessions = sessionRepository.findByEnrollmentOrderByScheduledAtAsc(enrollment);
        boolean hasCompleted = sessions.stream().anyMatch(session -> STATUS_COMPLETED.equalsIgnoreCase(session.getStatus()));
        boolean hasScheduled = sessions.stream().anyMatch(session -> STATUS_SCHEDULED.equalsIgnoreCase(session.getStatus()));

        if (hasCompleted) {
            enrollment.setHoursUsed(Math.max(1, nullToZero(enrollment.getHoursPurchased())));
            enrollment.setPaymentStatus(STATUS_COMPLETED);
        } else if (hasScheduled) {
            enrollment.setPaymentStatus(STATUS_SCHEDULED);
        } else if (STATUS_CANCELLED.equalsIgnoreCase(requestedStatus) && STATUS_SCHEDULED.equalsIgnoreCase(nullSafeStatus(enrollment))) {
            enrollment.setPaymentStatus(STATUS_ACCEPTED);
        }
        enrollmentRepository.save(enrollment);
    }

    private void notifyRequestCreated(ClassEnrollment enrollment) {
        String title = "Nueva solicitud de clase";
        String body = enrollment.getStudent().getName() + " te ha enviado una solicitud para clases.";
        notificationService.create(
                enrollment.getTeacher(),
                "class_request_created",
                title,
                body,
                Map.of(
                        "enrollmentId", enrollment.getId().toString(),
                        "studentId", enrollment.getStudent().getId().toString()
                ));
        emailService.sendClassNotificationEmail(
                enrollment.getTeacher().getEmail(),
                enrollment.getTeacher().getName(),
                title,
                body
        );
    }

    private void notifyStatusChange(ClassEnrollment enrollment, String status, User actor, String reason) {
        User recipient = STATUS_CANCELLED.equalsIgnoreCase(status) && actor != null
                && enrollment.getStudent().getId().equals(actor.getId())
                ? enrollment.getTeacher()
                : enrollment.getStudent();
        String title;
        String body;
        String notificationType;

        switch (status) {
            case STATUS_ACCEPTED -> {
                title = "Solicitud aceptada";
                body = enrollment.getTeacher().getName() + " ha aceptado tu solicitud de clase.";
                notificationType = "class_request_accepted";
            }
            case STATUS_REJECTED -> {
                title = "Solicitud rechazada";
                body = enrollment.getTeacher().getName() + " ha rechazado tu solicitud de clase."
                        + (reason == null || reason.isBlank() ? "" : " Motivo: " + reason);
                notificationType = "class_request_rejected";
            }
            case STATUS_COMPLETED -> {
                title = "Clase completada";
                body = "Tu clase con " + enrollment.getTeacher().getName() + " se ha marcado como completada.";
                notificationType = "class_request_completed";
            }
            case STATUS_CANCELLED -> {
                title = "Solicitud cancelada";
                body = "La solicitud con " + enrollment.getTeacher().getName() + " se ha cancelado.";
                notificationType = "class_request_cancelled";
            }
            default -> {
                return;
            }
        }

        notificationService.create(
                recipient,
                notificationType,
                title,
                body,
                Map.of(
                        "enrollmentId", enrollment.getId().toString(),
                        "teacherId", enrollment.getTeacher().getId().toString()
                ));
        emailService.sendClassNotificationEmail(
                recipient.getEmail(),
                recipient.getName(),
                title,
                body
        );
    }

    private void notifyStudentsAboutPost(ClassPost post) {
        Map<UUID, User> recipients = new LinkedHashMap<>();
        enrollmentRepository.findByTeacherOrderByCreatedAtDesc(post.getTeacher()).stream()
                .filter(enrollment -> {
                    String status = nullSafeStatus(enrollment);
                    return STATUS_ACCEPTED.equalsIgnoreCase(status)
                            || STATUS_PAID.equalsIgnoreCase(status)
                            || STATUS_SCHEDULED.equalsIgnoreCase(status)
                            || STATUS_COMPLETED.equalsIgnoreCase(status);
                })
                .forEach(enrollment -> recipients.putIfAbsent(enrollment.getStudent().getId(), enrollment.getStudent()));

        if (recipients.isEmpty()) {
            return;
        }

        String title = "Nueva noticia del profesor";
        String body = post.getTeacher().getName() + " ha publicado una noticia: " + post.getTitle();
        for (User student : recipients.values()) {
            notificationService.create(
                    student,
                    "class_post_published",
                    title,
                    body,
                    Map.of(
                            "teacherId", post.getTeacher().getId().toString(),
                            "postId", post.getId().toString()
                    ));
            emailService.sendClassNotificationEmail(
                    student.getEmail(),
                    student.getName(),
                    title,
                    body
            );
        }
    }

    private boolean isAllowedStatus(String status) {
        String normalized = nullSafeStatus(status);
        return STATUS_PENDING.equalsIgnoreCase(normalized)
                || STATUS_ACCEPTED.equalsIgnoreCase(normalized)
                || STATUS_REJECTED.equalsIgnoreCase(normalized)
                || STATUS_COMPLETED.equalsIgnoreCase(normalized)
                || STATUS_CANCELLED.equalsIgnoreCase(normalized)
                || STATUS_PAID.equalsIgnoreCase(normalized)
                || STATUS_SCHEDULED.equalsIgnoreCase(normalized)
                || STATUS_REFUND_REQUESTED.equalsIgnoreCase(normalized)
                || STATUS_REFUNDED.equalsIgnoreCase(normalized);
    }

    private boolean isActiveRequestStatus(String status) {
        String normalized = nullSafeStatus(status);
        return STATUS_PENDING.equalsIgnoreCase(normalized)
                || STATUS_ACCEPTED.equalsIgnoreCase(normalized)
                || STATUS_SCHEDULED.equalsIgnoreCase(normalized)
                || STATUS_PAID.equalsIgnoreCase(normalized);
    }

    private boolean isChatOpenState(String status) {
        String normalized = nullSafeStatus(status);
        return STATUS_ACCEPTED.equalsIgnoreCase(normalized)
                || STATUS_PAID.equalsIgnoreCase(normalized)
                || STATUS_SCHEDULED.equalsIgnoreCase(normalized)
                || STATUS_COMPLETED.equalsIgnoreCase(normalized)
                || STATUS_REFUND_REQUESTED.equalsIgnoreCase(normalized);
    }

    private boolean isSchedulableState(String status) {
        String normalized = nullSafeStatus(status);
        return STATUS_ACCEPTED.equalsIgnoreCase(normalized)
                || STATUS_PAID.equalsIgnoreCase(normalized)
                || STATUS_SCHEDULED.equalsIgnoreCase(normalized);
    }

    private void notifyScheduling(ClassEnrollment enrollment, ClassSession session) {
        String title = "Clase agendada";
        String body = enrollment.getTeacher().getName() + " te ha propuesto una clase para el "
                + session.getScheduledAt() + ".";

        notificationService.create(
                enrollment.getStudent(),
                "class_scheduled",
                title,
                body,
                Map.of(
                        "enrollmentId", enrollment.getId().toString(),
                        "sessionId", session.getId().toString()
                ));

        emailService.sendClassNotificationEmail(
                enrollment.getStudent().getEmail(),
                enrollment.getStudent().getName(),
                title,
                body
        );
    }

    private void notifySessionUpdate(ClassSession session) {
        String title = STATUS_COMPLETED.equalsIgnoreCase(session.getStatus())
                ? "Clase completada"
                : "Actualizacion de tu clase";
        String body = STATUS_COMPLETED.equalsIgnoreCase(session.getStatus())
                ? "Tu clase con " + session.getTeacher().getName() + " ha sido marcada como completada."
                : "Tu profesor ha actualizado la sesion del " + session.getScheduledAt() + ".";

        notificationService.create(
                session.getStudent(),
                "class_session_update",
                title,
                body,
                Map.of(
                        "enrollmentId", session.getEnrollment().getId().toString(),
                        "sessionId", session.getId().toString()
                ));

        emailService.sendClassNotificationEmail(
                session.getStudent().getEmail(),
                session.getStudent().getName(),
                title,
                body
        );
    }

    private Map<String, Object> toEnrollmentMap(ClassEnrollment enrollment) {
        ClassSession nextSession = sessionRepository
                .findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(enrollment, STATUS_SCHEDULED)
                .orElse(null);

        int hoursPurchased = nullToZero(enrollment.getHoursPurchased());
        int hoursUsed = nullToZero(enrollment.getHoursUsed());
        int hoursRemaining = Math.max(hoursPurchased - hoursUsed, 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", value(enrollment.getId()));
        result.put("teacherId", value(enrollment.getTeacher() != null ? enrollment.getTeacher().getId() : null));
        result.put("teacherName", safeString(enrollment.getTeacherName()));
        result.put("studentId", value(enrollment.getStudent() != null ? enrollment.getStudent().getId() : null));
        result.put("studentName", safeString(enrollment.getStudentName()));
        result.put("instrument", safeNullable(enrollment.getInstrument()));
        result.put("city", safeString(enrollment.getCity()));
        result.put("province", safeString(enrollment.getProvince()));
        result.put("modality", safeString(enrollment.getModality()));
        result.put("requestedModality", safeString(enrollment.getRequestedModality()));
        result.put("unitPrice", enrollment.getUnitPrice() != null ? enrollment.getUnitPrice() : BigDecimal.ZERO);
        result.put("paymentStatus", safeString(enrollment.getPaymentStatus()));
        result.put("requestMessage", safeString(enrollment.getRequestMessage()));
        result.put("requestAvailability", safeString(enrollment.getRequestAvailability()));
        result.put("classLink", safeString(enrollment.getClassLink()));
        result.put("createdAt", enrollment.getCreatedAt() != null ? enrollment.getCreatedAt().toString() : "");
        result.put("teacherRole", enrollment.getTeacher() != null && enrollment.getTeacher().getRole() != null
                ? enrollment.getTeacher().getRole().name().toLowerCase()
                : "");
        result.put("hoursPurchased", hoursPurchased);
        result.put("hoursUsed", hoursUsed);
        result.put("hoursRemaining", hoursRemaining);
        result.put("canRefund", false);
        result.put("canChat", isChatOpenState(enrollment.getPaymentStatus()) && !STATUS_REFUNDED.equalsIgnoreCase(nullSafeStatus(enrollment)));
        result.put("nextSession", nextSession != null ? toSessionMap(nextSession) : null);
        return result;
    }

    private Map<String, Object> toMessageMap(ClassMessage message) {
        return Map.of(
                "id", message.getId().toString(),
                "enrollmentId", message.getEnrollment().getId().toString(),
                "senderId", message.getSender().getId().toString(),
                "senderName", message.getSender().getName(),
                "content", message.getContent(),
                "createdAt", message.getCreatedAt().toString()
        );
    }

    private Map<String, Object> toPostMap(ClassPost post) {
        return Map.of(
                "id", post.getId().toString(),
                "teacherId", post.getTeacher().getId().toString(),
                "teacherName", post.getTeacher().getName(),
                "title", post.getTitle(),
                "content", post.getContent(),
                "createdAt", post.getCreatedAt().toString()
        );
    }

    private Map<String, Object> toSessionMap(ClassSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", session.getId().toString());
        result.put("enrollmentId", session.getEnrollment().getId().toString());
        result.put("teacherId", session.getTeacher().getId().toString());
        result.put("teacherName", session.getTeacher().getName());
        result.put("studentId", session.getStudent().getId().toString());
        result.put("studentName", session.getStudent().getName());
        result.put("scheduledAt", session.getScheduledAt().toString());
        result.put("durationMinutes", session.getDurationMinutes());
        result.put("status", safeString(session.getStatus()));
        result.put("meetingUrl", safeString(session.getMeetingUrl()));
        result.put("notes", safeString(session.getNotes()));
        return result;
    }

    private int nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return 60;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : 60;
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    private String nullSafeStatus(ClassEnrollment enrollment) {
        return nullSafeStatus(enrollment != null ? enrollment.getPaymentStatus() : null);
    }

    private String nullSafeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private Object safeNullable(Object value) {
        return value;
    }

    private String value(UUID id) {
        return id != null ? id.toString() : "";
    }
}
