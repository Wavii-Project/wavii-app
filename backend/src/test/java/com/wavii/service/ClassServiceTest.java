package com.wavii.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.wavii.model.ClassEnrollment;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private ClassMessageRepository messageRepository;
    @Mock private ClassPostRepository postRepository;
    @Mock private ClassSessionRepository sessionRepository;
    @Mock private TeacherReportRepository teacherReportRepository;
    @Mock private StripeService stripeService;
    @Mock private OdooService odooService;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    @InjectMocks private ClassService classService;

    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(UUID.randomUUID());
        teacher.setName("Prof. Ana");
        teacher.setEmail("ana@wavii.app");
        teacher.setRole(Role.PROFESOR_CERTIFICADO);
        teacher.setPricePerHour(new BigDecimal("25.00"));
        teacher.setClassModality("ONLINE");

        student = new User();
        student.setId(UUID.randomUUID());
        student.setName("Alumno");
        student.setEmail("alumno@wavii.app");
        student.setRole(Role.USUARIO);

    }

    @Test
    void requestClassCreatesPendingEnrollmentTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findFirstByTeacherAndStudentOrderByCreatedAtDesc(eq(teacher), eq(student)))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        Map<String, Object> result = classService.requestClass(
                teacher.getId(),
                student,
                Map.of("message", "Hola", "availability", "Martes tarde", "requestedModality", "ONLINE")
        );

        assertEquals("pending", result.get("paymentStatus"));
        assertEquals("Hola", result.get("requestMessage"));
        assertEquals("Martes tarde", result.get("requestAvailability"));
        assertEquals("ONLINE", result.get("requestedModality"));
    }

    @Test
    void updateStatusAcceptsRequestForTeacherTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = classService.updateStatus(
                enrollment.getId(),
                teacher,
                Map.of("status", "accepted")
        );

        assertEquals("accepted", result.get("paymentStatus"));
        assertTrue(Boolean.TRUE.equals(result.get("canChat")));
    }

    @Test
    void updateStatusRejectsRequestWithReasonInNotificationAndEmailTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = classService.updateStatus(
                enrollment.getId(),
                teacher,
                Map.of("status", "rejected", "reason", "Horario incompatible")
        );

        assertEquals("rejected", result.get("paymentStatus"));
        verify(notificationService).create(
                eq(student),
                eq("class_request_rejected"),
                eq("Solicitud rechazada"),
                eq("Prof. Ana ha rechazado tu solicitud de clase. Motivo: Horario incompatible"),
                any()
        );
        verify(emailService).sendClassNotificationEmail(
                eq(student.getEmail()),
                eq(student.getName()),
                eq("Solicitud rechazada"),
                eq("Prof. Ana ha rechazado tu solicitud de clase. Motivo: Horario incompatible")
        );
    }

    @Test
    void getPostsForStudentReturnsPostsFromActiveTeachersWithoutDuplicatesTest() {
        ClassEnrollment activeOne = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();
        ClassEnrollment activeTwo = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("scheduled")
                .build();
        ClassEnrollment rejected = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("rejected")
                .build();

        ClassPost newest = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Nuevo horario")
                .content("Hay huecos el jueves")
                .createdAt(LocalDateTime.now())
                .build();
        ClassPost older = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Aviso anterior")
                .content("Contenido anterior")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student))
                .thenReturn(List.of(activeOne, activeTwo, rejected));
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher))
                .thenReturn(List.of(newest, older));

        List<Map<String, Object>> result = classService.getPostsForStudent(student);

        assertEquals(2, result.size());
        assertEquals("Nuevo horario", result.get(0).get("title"));
        assertEquals("Aviso anterior", result.get(1).get("title"));
        verify(postRepository, times(1)).findByTeacherOrderByCreatedAtDesc(teacher);
    }

    @Test
    void createPostNotifiesActiveStudentsOnceTest() {
        User inactiveStudent = new User();
        inactiveStudent.setId(UUID.randomUUID());
        inactiveStudent.setName("Alumno inactivo");
        inactiveStudent.setEmail("inactivo@wavii.app");
        inactiveStudent.setRole(Role.USUARIO);

        ClassEnrollment activeOne = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();
        ClassEnrollment activeTwo = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("completed")
                .build();
        ClassEnrollment rejected = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(inactiveStudent)
                .paymentStatus("rejected")
                .build();

        when(postRepository.save(any(ClassPost.class))).thenAnswer(invocation -> {
            ClassPost post = invocation.getArgument(0);
            post.setId(UUID.randomUUID());
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });
        when(enrollmentRepository.findByTeacherOrderByCreatedAtDesc(teacher))
                .thenReturn(List.of(activeOne, activeTwo, rejected));

        Map<String, Object> result = classService.createPost(teacher, "Novedad", "Texto para alumnos");

        assertEquals("Novedad", result.get("title"));
        verify(notificationService, times(1)).create(eq(student), eq("class_post_published"), any(), any(), any());
        verify(emailService, times(1)).sendClassNotificationEmail(eq(student.getEmail()), eq(student.getName()), any(), any());
        verify(notificationService, never()).create(eq(inactiveStudent), eq("class_post_published"), any(), any(), any());
        verify(emailService, never()).sendClassNotificationEmail(eq(inactiveStudent.getEmail()), eq(inactiveStudent.getName()), any(), any());
    }

    // ─── checkout ────────────────────────────────────────────────────────────

    @Test
    void checkoutNullStudentThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.checkout(teacher.getId(), null));
    }

    @Test
    void checkoutTeacherNotFoundThrowsTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> classService.checkout(unknownId, student));
    }

    @Test
    void checkoutUserIsNotTeacherThrowsTest() {
        User notTeacher = new User();
        notTeacher.setId(UUID.randomUUID());
        notTeacher.setName("Normal");
        notTeacher.setEmail("normal@wavii.app");
        notTeacher.setRole(Role.USUARIO);

        when(userRepository.findById(notTeacher.getId())).thenReturn(Optional.of(notTeacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.checkout(notTeacher.getId(), student));
    }

    @Test
    void checkoutSelfSubscriptionThrowsTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.checkout(teacher.getId(), teacher));
    }

//     @Test
//     void checkoutPriceZeroNoStripeMarksPaidTest() throws Exception {
//         teacher.setPricePerHour(BigDecimal.ZERO);
//         when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
//         when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
//             ClassEnrollment e = invocation.getArgument(0);
//             e.setId(UUID.randomUUID());
//             return e;
//         });
//         when(stripeService.isConfigured()).thenReturn(false);

//         Map<String, Object> result = classService.checkout(teacher.getId(), student);

//         assertEquals("free_class_no_payment", result.get("clientSecret"));
//         assertTrue(Boolean.TRUE.equals(result.get("devMode")));
//     }

    @Test
    void checkoutStripeNotConfiguredMarksPaidDevModeTest() throws Exception {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(stripeService.isConfigured()).thenReturn(false);

        Map<String, Object> result = classService.checkout(teacher.getId(), student);

        assertEquals("dev_class_client_secret", result.get("clientSecret"));
        assertTrue(Boolean.TRUE.equals(result.get("devMode")));
    }

    @Test
    void checkoutStripeConfiguredReturnsPaymentIntentTest() throws Exception {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.createOrGetCustomer(student)).thenReturn("cus_test123");
        when(userRepository.save(student)).thenReturn(student);
        when(stripeService.createClassPaymentIntent(
                eq("cus_test123"), any(), any(), any(), any(), any(), any(), any(Long.class)))
                .thenReturn(Map.of("paymentIntentId", "pi_test123", "clientSecret", "cs_test"));

        Map<String, Object> result = classService.checkout(teacher.getId(), student);

        assertEquals("pi_test123", result.get("paymentIntentId"));
        assertEquals("cs_test", result.get("clientSecret"));
        assertFalse(Boolean.TRUE.equals(result.get("devMode")));
    }

    // ─── confirm ─────────────────────────────────────────────────────────────

    @Test
    void confirmAlreadyPaidReturnsEnrollmentMapTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = classService.confirm(enrollment.getId(), student);

        assertEquals("paid", result.get("paymentStatus"));
    }

    @Test
    void confirmPendingNoStripeMarksPaidTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .stripePaymentIntentId(null)
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = classService.confirm(enrollment.getId(), student);

        assertEquals("paid", result.get("paymentStatus"));
    }

    @Test
    void confirmPendingStripeNoIntentIdThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .stripePaymentIntentId(null)
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> classService.confirm(enrollment.getId(), student));
    }

    @Test
    void confirmStripePaymentSucceededMarksPaidTest() throws StripeException {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .stripePaymentIntentId("pi_real123")
                .build();

        PaymentIntent pi = new PaymentIntent();
        pi.setStatus("succeeded");
        pi.setLatestCharge("ch_test");

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrievePaymentIntent("pi_real123")).thenReturn(pi);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = classService.confirm(enrollment.getId(), student);

        assertEquals("paid", result.get("paymentStatus"));
    }

    @Test
    void confirmStripePaymentNotSucceededThrowsTest() throws StripeException {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .stripePaymentIntentId("pi_pending123")
                .build();

        PaymentIntent pi = new PaymentIntent();
        pi.setStatus("requires_payment_method");

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrievePaymentIntent("pi_pending123")).thenReturn(pi);

        assertThrows(IllegalArgumentException.class,
                () -> classService.confirm(enrollment.getId(), student));
    }

    // ─── getMessages / sendMessage ────────────────────────────────────────────

    @Test
    void getMessagesNonParticipantThrowsTest() {
        User stranger = new User();
        stranger.setId(UUID.randomUUID());
        stranger.setName("Stranger");
        stranger.setEmail("stranger@wavii.app");

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.getMessages(enrollment.getId(), stranger));
    }

    @Test
    void getMessagesChatNotEnabledThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.getMessages(enrollment.getId(), student));
    }

    @Test
    void sendMessageEmptyContentThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.sendMessage(enrollment.getId(), student, "   "));
    }

    @Test
    void sendMessageNullContentThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.sendMessage(enrollment.getId(), student, null));
    }

    // ─── getPostsForViewer ────────────────────────────────────────────────────

    @Test
    void getPostsForViewerTeacherNotFoundThrowsTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> classService.getPostsForViewer(student, unknownId));
    }

    @Test
    void getPostsForViewerNotATeacherReturnsEmptyTest() {
        User notTeacher = new User();
        notTeacher.setId(UUID.randomUUID());
        notTeacher.setName("Normal");
        notTeacher.setEmail("normal@wavii.app");
        notTeacher.setRole(Role.USUARIO);

        when(userRepository.findById(notTeacher.getId())).thenReturn(Optional.of(notTeacher));

        List<Map<String, Object>> result = classService.getPostsForViewer(student, notTeacher.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void getPostsForViewerNullViewerThrowsTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.getPostsForViewer(null, teacher.getId()));
    }

    @Test
    void getPostsForViewerViewerIsTeacherReturnsPostsTest() {
        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Nota interna")
                .content("Solo para mi")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPostsForViewer(teacher, teacher.getId());

        assertEquals(1, result.size());
        assertEquals("Nota interna", result.get(0).get("title"));
    }

    @Test
    void getPostsForViewerEnrolledStudentReturnsPostsTest() {
        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Aviso")
                .content("Contenido")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "accepted"))
                .thenReturn(true);
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPostsForViewer(student, teacher.getId());

        assertEquals(1, result.size());
    }

    @Test
    void getPostsForViewerNotEnrolledStudentThrowsTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "accepted"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "paid"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "scheduled"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "completed"))
                .thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> classService.getPostsForViewer(student, teacher.getId()));
    }

    // ─── getPostsForStudent ───────────────────────────────────────────────────

    @Test
    void getPostsForStudentNullStudentThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.getPostsForStudent(null));
    }

    @Test
    void getPostsForStudentNoActiveEnrollmentsReturnsEmptyTest() {
        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student)).thenReturn(List.of());

        List<Map<String, Object>> result = classService.getPostsForStudent(student);

        assertTrue(result.isEmpty());
    }

    // ─── createPost ───────────────────────────────────────────────────────────

    @Test
    void createPostNotTeacherThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.createPost(student, "Titulo", "Contenido"));
    }

    @Test
    void createPostBlankTitleThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.createPost(teacher, "   ", "Contenido"));
    }

    @Test
    void createPostBlankContentThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.createPost(teacher, "Titulo", ""));
    }

    @Test
    void createPostNullTitleThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.createPost(teacher, null, "Contenido"));
    }

    // ─── requestRefund ────────────────────────────────────────────────────────

    @Test
    void requestRefundNotStudentThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestRefund(enrollment.getId(), teacher));
    }

    @Test
    void requestRefundNotPaidStateThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestRefund(enrollment.getId(), student));
    }

    @Test
    void requestRefundCompletedStateThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("completed")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestRefund(enrollment.getId(), student));
    }

    @Test
    void requestRefundStripeNotConfiguredRefundsLocallyTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .stripePaymentIntentId("dev_pi_abc")
                .unitPrice(new BigDecimal("25.00"))
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = classService.requestRefund(enrollment.getId(), student);

        assertEquals("refunded", result.get("paymentStatus"));
        verify(odooService).processClassRefund(any(), any(), any(), any(Double.class));
        verify(notificationService).create(eq(teacher), eq("class_refund"), any(), any(), any());
    }

    @Test
    void requestRefundFreeClassSkipsStripeTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .stripePaymentIntentId("free_class_abc")
                .unitPrice(BigDecimal.ZERO)
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = classService.requestRefund(enrollment.getId(), student);

        assertEquals("refunded", result.get("paymentStatus"));
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    void updateStatusStudentCancelsPendingAllowedTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = classService.updateStatus(
                enrollment.getId(), student, Map.of("status", "cancelled"));

        assertEquals("cancelled", result.get("paymentStatus"));
    }

    @Test
    void updateStatusInvalidStatusThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.updateStatus(enrollment.getId(), teacher, Map.of("status", "invalid_state")));
    }

    @Test
    void updateStatusPendingToCompletedByTeacherThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.updateStatus(enrollment.getId(), teacher, Map.of("status", "completed")));
    }

    @Test
    void updateStatusStudentTriesToAcceptThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.updateStatus(enrollment.getId(), student, Map.of("status", "accepted")));
    }

    @Test
    void updateStatusNullStatusThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("status", null);

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.updateStatus(enrollment.getId(), teacher, body));
    }

    // ─── createSession ────────────────────────────────────────────────────────

    @Test
    void createSessionMissingScheduledAtThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("scheduledAt", null);

        assertThrows(IllegalArgumentException.class,
                () -> classService.createSession(enrollment.getId(), teacher, body));
    }

//     @Test
//     void createSessionUpdatesEnrollmentToScheduledTest() {
//         ClassEnrollment enrollment = ClassEnrollment.builder()
//                 .id(UUID.randomUUID())
//                 .teacher(teacher)
//                 .student(student)
//                 .paymentStatus("accepted")
//                 .build();

//         ClassSession savedSession = ClassSession.builder()
//                 .id(UUID.randomUUID())
//                 .enrollment(enrollment)
//                 .teacher(teacher)
//                 .student(student)
//                 .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
//                 .status("scheduled")
//                 .build();

//         when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
//         when(sessionRepository.save(any(ClassSession.class))).thenReturn(savedSession);
//         when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

//         Map<String, Object> result = classService.createSession(
//                 enrollment.getId(),
//                 teacher,
//                 Map.of("scheduledAt", "2026-06-01T10:00:00")
//         );

//         assertNotNull(result.get("id"));
//         verify(enrollmentRepository, times(2)).save(any());
//     }

    @Test
    void createSessionWrongStatusThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.createSession(enrollment.getId(), teacher,
                        Map.of("scheduledAt", "2026-06-01T10:00:00")));
    }

    // ─── updateSession ────────────────────────────────────────────────────────

    @Test
    void updateSessionStatusCompletedSyncsEnrollmentTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("scheduled")
                .hoursPurchased(1)
                .hoursUsed(0)
                .build();

        ClassSession session = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .status("scheduled")
                .build();

        ClassSession completedSession = ClassSession.builder()
                .id(session.getId())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .status("completed")
                .build();

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(completedSession);
        when(sessionRepository.findByEnrollmentOrderByScheduledAtAsc(enrollment))
                .thenReturn(List.of(completedSession));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = classService.updateSession(
                session.getId(),
                teacher,
                Map.of("status", "completed")
        );

        assertEquals("completed", result.get("status"));
        verify(enrollmentRepository).save(any());
    }

    @Test
    void updateSessionNotFoundThrowsTest() {
        UUID unknownId = UUID.randomUUID();
        when(sessionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> classService.updateSession(unknownId, teacher, Map.of("status", "completed")));
    }

    // ─── ensureTeacherOwner ───────────────────────────────────────────────────

    @Test
    void ensureTeacherOwnerNullUserThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.ensureTeacherOwner(teacher, null));
    }

    @Test
    void ensureTeacherOwnerDifferentUserThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.ensureTeacherOwner(teacher, student));
    }

    @Test
    void ensureTeacherOwnerSameUserPassesTest() {
        assertDoesNotThrow(() -> classService.ensureTeacherOwner(teacher, teacher));
    }

    // ─── createTeacherReport ─────────────────────────────────────────────────

    @Test
    void createTeacherReportTeacherNotFoundThrowsTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> classService.createTeacherReport(unknownId, student, Map.of("reason", "Mal comportamiento")));
    }

    @Test
    void createTeacherReportTargetNotTeacherThrowsTest() {
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
                () -> classService.createTeacherReport(student.getId(), teacher, Map.of("reason", "Prueba")));
    }

    @Test
    void createTeacherReportNullReporterThrowsTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.createTeacherReport(teacher.getId(), null, Map.of("reason", "Prueba")));
    }

    @Test
    void createTeacherReportSelfReportThrowsTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.createTeacherReport(teacher.getId(), teacher, Map.of("reason", "Prueba")));
    }

    @Test
    void createTeacherReportNullReasonThrowsTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("reason", null);

        assertThrows(IllegalArgumentException.class,
                () -> classService.createTeacherReport(teacher.getId(), student, body));
    }

    @Test
    void createTeacherReportSuccessReturnsIdAndStatusTest() {
        TeacherReport savedReport = TeacherReport.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .reporter(student)
                .reason("Actitud inapropiada")
                .status("open")
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(teacherReportRepository.save(any(TeacherReport.class))).thenReturn(savedReport);

        Map<String, Object> result = classService.createTeacherReport(
                teacher.getId(), student, Map.of("reason", "Actitud inapropiada"));

        assertNotNull(result.get("id"));
        assertEquals("open", result.get("status"));
    }
}
