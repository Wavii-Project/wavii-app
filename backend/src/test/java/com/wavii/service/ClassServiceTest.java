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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    // ─── listClasses ─────────────────────────────────────────────────────────

    @Test
    void listClassesReturnsEnrollmentsForStudentTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("accepted")
                .hoursPurchased(1)
                .hoursUsed(0)
                .build();
        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student)).thenReturn(List.of(enrollment));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        List<Map<String, Object>> result = classService.listClasses(student);

        assertEquals(1, result.size());
        assertEquals("accepted", result.get(0).get("paymentStatus"));
    }

    @Test
    void listClassesEmptyReturnsEmptyListTest() {
        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student)).thenReturn(List.of());

        List<Map<String, Object>> result = classService.listClasses(student);

        assertTrue(result.isEmpty());
    }

    // ─── getManageOverview ────────────────────────────────────────────────────

    @Test
    void getManageOverviewNotTeacherThrowsTest() {
        assertThrows(IllegalArgumentException.class, () -> classService.getManageOverview(student));
    }

    @Test
    void getManageOverviewTeacherReturnsMapTest() {
        when(enrollmentRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of());
        when(sessionRepository.findByTeacherOrderByScheduledAtAsc(teacher)).thenReturn(List.of());
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of());

        Map<String, Object> result = classService.getManageOverview(teacher);

        assertNotNull(result);
        assertTrue(result.containsKey("classes"));
        assertTrue(result.containsKey("sessions"));
        assertTrue(result.containsKey("posts"));
    }

    // ─── requestClass with active request ────────────────────────────────────

    @Test
    void requestClassExistingActiveRequestThrowsTest() {
        ClassEnrollment existing = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findFirstByTeacherAndStudentOrderByCreatedAtDesc(eq(teacher), eq(student)))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestClass(teacher.getId(), student, Map.of("message", "Hola")));
    }

    // ─── confirm with StripeException ────────────────────────────────────────

    @Test
    void confirmStripeExceptionThrowsIllegalArgumentTest() throws StripeException {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .stripePaymentIntentId("pi_error")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);
        when(stripeService.retrievePaymentIntent("pi_error"))
                .thenThrow(new com.stripe.exception.ApiConnectionException("Stripe error", null));

        assertThrows(IllegalArgumentException.class,
                () -> classService.confirm(enrollment.getId(), student));
    }

    // ─── updateStatus completed/cancelled notifications ───────────────────────

    @Test
    void updateStatusCompletedSendsNotificationTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = classService.updateStatus(
                enrollment.getId(), teacher, Map.of("status", "completed"));

        assertEquals("completed", result.get("paymentStatus"));
        verify(notificationService).create(eq(student), eq("class_request_completed"), any(), any(), any());
    }

    @Test
    void updateStatusCancelledByStudentNotifiesTeacherTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = classService.updateStatus(
                enrollment.getId(), student, Map.of("status", "cancelled"));

        assertEquals("cancelled", result.get("paymentStatus"));
        verify(notificationService).create(eq(teacher), eq("class_request_cancelled"), any(), any(), any());
    }

    @Test
    void updateStatusAcceptedSendsNotificationTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        classService.updateStatus(enrollment.getId(), teacher, Map.of("status", "accepted"));

        verify(notificationService).create(eq(student), eq("class_request_accepted"), any(), any(), any());
    }

    @Test
    void updateStatusUnknownStatusDefaultsNoNotificationTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        classService.updateStatus(enrollment.getId(), teacher, Map.of("status", "refunded"));

        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }

    // ─── createSession success ────────────────────────────────────────────────

    @Test
    void createSessionSuccessUpdatesEnrollmentToScheduledTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        ClassSession savedSession = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .status("scheduled")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(savedSession);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = classService.createSession(
                enrollment.getId(), teacher, Map.of("scheduledAt", "2026-06-01T10:00:00"));

        assertNotNull(result.get("id"));
        verify(enrollmentRepository, times(1)).save(any());
    }

    // ─── updateSession field updates ──────────────────────────────────────────

    @Test
    void updateSessionUpdatesScheduledAtDurationMeetingUrlNotesTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("scheduled")
                .build();

        ClassSession session = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .status("scheduled")
                .build();

        ClassSession updatedSession = ClassSession.builder()
                .id(session.getId())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 7, 1, 12, 0))
                .durationMinutes(90)
                .meetingUrl("https://meet.example.com")
                .notes("Bring instrument")
                .status("scheduled")
                .build();

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(updatedSession);
        when(sessionRepository.findByEnrollmentOrderByScheduledAtAsc(enrollment))
                .thenReturn(List.of(updatedSession));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> body = new HashMap<>();
        body.put("scheduledAt", "2026-07-01T12:00:00");
        body.put("durationMinutes", "90");
        body.put("meetingUrl", "https://meet.example.com");
        body.put("notes", "Bring instrument");

        Map<String, Object> result = classService.updateSession(session.getId(), teacher, body);

        assertNotNull(result);
        assertEquals("https://meet.example.com", result.get("meetingUrl"));
    }

    // ─── syncEnrollmentWithSessions ──────────────────────────────────────────

    @Test
    void updateSessionCancelledWhenScheduledMovesBackToAcceptedTest() {
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
                .status("cancelled")
                .build();

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(session);
        when(sessionRepository.findByEnrollmentOrderByScheduledAtAsc(enrollment))
                .thenReturn(List.of(session));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        classService.updateSession(session.getId(), teacher, Map.of("status", "cancelled"));

        verify(enrollmentRepository).save(any());
    }

    @Test
    void updateSessionScheduledKeepsScheduledEnrollmentTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
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

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(session);
        when(sessionRepository.findByEnrollmentOrderByScheduledAtAsc(enrollment))
                .thenReturn(List.of(session));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        classService.updateSession(session.getId(), teacher, Map.of("status", "scheduled"));

        verify(enrollmentRepository).save(any());
    }

    // ─── sendMessage success ──────────────────────────────────────────────────

    @Test
    void sendMessageSuccessSavesMessageTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        ClassMessage savedMessage = ClassMessage.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .sender(teacher)
                .content("Hola!")
                .build();
        savedMessage.setCreatedAt(LocalDateTime.now());

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(messageRepository.save(any(ClassMessage.class))).thenReturn(savedMessage);

        Map<String, Object> result = classService.sendMessage(enrollment.getId(), teacher, "Hola!");

        assertNotNull(result);
        assertEquals("Hola!", result.get("content"));
    }

    // ─── getMessages success ──────────────────────────────────────────────────

    @Test
    void getMessagesSuccessReturnsMessagesTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        ClassMessage message = ClassMessage.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .sender(teacher)
                .content("Hello")
                .build();
        message.setCreatedAt(LocalDateTime.now());

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(messageRepository.findByEnrollmentOrderByCreatedAtAsc(enrollment)).thenReturn(List.of(message));

        List<Map<String, Object>> result = classService.getMessages(enrollment.getId(), teacher);

        assertEquals(1, result.size());
    }

    // ─── getPosts ─────────────────────────────────────────────────────────────

    @Test
    void getPostsNotTeacherReturnsEmptyTest() {
        List<Map<String, Object>> result = classService.getPosts(student);
        assertTrue(result.isEmpty());
    }

    @Test
    void getPostsTeacherReturnsPostsTest() {
        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Aviso")
                .content("Contenido")
                .createdAt(LocalDateTime.now())
                .build();
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPosts(teacher);

        assertEquals(1, result.size());
    }

    // ─── checkout free price ──────────────────────────────────────────────────

    @Test
    void checkoutFreePriceMarksPaidTest() throws Exception {
        teacher.setPricePerHour(BigDecimal.ZERO);
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        Map<String, Object> result = classService.checkout(teacher.getId(), student);

        assertEquals("free_class_no_payment", result.get("clientSecret"));
        assertTrue(Boolean.TRUE.equals(result.get("devMode")));
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

    // ─── requestExtraHour ─────────────────────────────────────────────────────

    @Test
    void requestExtraHourCallsCheckoutForTeacherTest() throws Exception {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(stripeService.isConfigured()).thenReturn(false);
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });

        Map<String, Object> result = classService.requestExtraHour(enrollment.getId(), student);

        assertNotNull(result);
        assertEquals("dev_class_client_secret", result.get("clientSecret"));
    }

    @Test
    void requestExtraHourNotStudentThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestExtraHour(enrollment.getId(), teacher));
    }

    // ─── checkout null pricePerHour ────────────────────────────────────────────

    @Test
    void checkoutNullPriceThrowsTest() {
        teacher.setPricePerHour(null);
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.checkout(teacher.getId(), student));
    }

    // ─── requestClass null student ─────────────────────────────────────────────

    @Test
    void requestClassNullStudentThrowsTest() {
        assertThrows(IllegalArgumentException.class,
                () -> classService.requestClass(teacher.getId(), null, Map.of()));
    }

    @Test
    void requestClassTeacherNotFoundThrowsTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestClass(unknownId, student, Map.of()));
    }

    @Test
    void requestClassUserNotTeacherThrowsTest() {
        User notTeacher = new User();
        notTeacher.setId(UUID.randomUUID());
        notTeacher.setRole(Role.USUARIO);

        when(userRepository.findById(notTeacher.getId())).thenReturn(Optional.of(notTeacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestClass(notTeacher.getId(), student, Map.of()));
    }

    @Test
    void requestClassSelfSubscriptionThrowsTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestClass(teacher.getId(), teacher, Map.of()));
    }

    @Test
    void requestClassNullRequestedModalityUsesTeacherModalityTest() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findFirstByTeacherAndStudentOrderByCreatedAtDesc(eq(teacher), eq(student)))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("message", null);
        body.put("availability", null);
        body.put("requestedModality", null);

        Map<String, Object> result = classService.requestClass(teacher.getId(), student, body);

        assertEquals("ONLINE", result.get("requestedModality"));
    }

    // ─── confirm non-pending status (not isPaid, not pending → throws) ─────────

    @Test
    void confirmRejectedStatusThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("rejected")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.confirm(enrollment.getId(), student));
    }

    // ─── updateStatus student tries rejected ──────────────────────────────────

    @Test
    void updateStatusStudentTriesToRejectThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.updateStatus(enrollment.getId(), student, Map.of("status", "rejected")));
    }

    // ─── getPostsForStudent with null teacher in enrollment ────────────────────

    @Test
    void getPostsForStudentEnrollmentWithNullTeacherSkipsTest() {
        ClassEnrollment activeEnrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(null)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student))
                .thenReturn(List.of(activeEnrollment));

        List<Map<String, Object>> result = classService.getPostsForStudent(student);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ─── getPostsForStudent with paid/scheduled/completed statuses ─────────────

    @Test
    void getPostsForStudentPaidStatusIncludedTest() {
        ClassEnrollment paidEnrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .build();

        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Titulo")
                .content("Contenido")
                .createdAt(LocalDateTime.now())
                .build();

        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student))
                .thenReturn(List.of(paidEnrollment));
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPostsForStudent(student);

        assertEquals(1, result.size());
    }

    @Test
    void getPostsForStudentCompletedStatusIncludedTest() {
        ClassEnrollment completedEnrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("completed")
                .build();

        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Completada")
                .content("Contenido")
                .createdAt(LocalDateTime.now())
                .build();

        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student))
                .thenReturn(List.of(completedEnrollment));
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPostsForStudent(student);

        assertEquals(1, result.size());
    }

    // ─── getPostsForViewer with paid/scheduled/completed enrolled ─────────────

    @Test
    void getPostsForViewerPaidEnrolledStudentReturnsPostsTest() {
        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Aviso")
                .content("Contenido")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "accepted"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "paid"))
                .thenReturn(true);
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPostsForViewer(student, teacher.getId());

        assertEquals(1, result.size());
    }

    @Test
    void getPostsForViewerScheduledEnrolledStudentReturnsPostsTest() {
        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Aviso")
                .content("Contenido")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "accepted"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "paid"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "scheduled"))
                .thenReturn(true);
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPostsForViewer(student, teacher.getId());

        assertEquals(1, result.size());
    }

    @Test
    void getPostsForViewerCompletedEnrolledStudentReturnsPostsTest() {
        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Aviso")
                .content("Contenido")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "accepted"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "paid"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "scheduled"))
                .thenReturn(false);
        when(enrollmentRepository.existsByTeacherAndStudentAndPaymentStatusIgnoreCase(teacher, student, "completed"))
                .thenReturn(true);
        when(postRepository.findByTeacherOrderByCreatedAtDesc(teacher)).thenReturn(List.of(post));

        List<Map<String, Object>> result = classService.getPostsForViewer(student, teacher.getId());

        assertEquals(1, result.size());
    }

    // ─── requestRefund with Stripe configured and real pi ─────────────────────

    @Test
    void requestRefundStripeConfiguredAndRealPiCallsStripeRefundTest() throws Exception {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .stripePaymentIntentId("pi_real_123")
                .unitPrice(new BigDecimal("25.00"))
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = classService.requestRefund(enrollment.getId(), student);

        assertEquals("refunded", result.get("paymentStatus"));
        verify(stripeService).refundPaymentIntent("pi_real_123");
    }

    @Test
    void requestRefundStripeExceptionThrowsIllegalArgumentTest() throws Exception {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .stripePaymentIntentId("pi_fail")
                .unitPrice(new BigDecimal("25.00"))
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);
        doThrow(new com.stripe.exception.ApiConnectionException("Stripe error", null))
                .when(stripeService).refundPaymentIntent("pi_fail");

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestRefund(enrollment.getId(), student));
    }

    @Test
    void requestRefundNullUnitPriceUsesZeroTest() throws Exception {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .stripePaymentIntentId("free_class_xyz")
                .unitPrice(null)
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(stripeService.isConfigured()).thenReturn(true);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        Map<String, Object> result = classService.requestRefund(enrollment.getId(), student);

        assertEquals("refunded", result.get("paymentStatus"));
        verify(odooService).processClassRefund(any(), any(), any(), eq(0.0));
    }

    // ─── createSession with duration parse edge cases ──────────────────────────

    @Test
    void createSessionInvalidDurationDefaultsSixtyTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        ClassSession savedSession = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .durationMinutes(60)
                .status("scheduled")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(savedSession);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("scheduledAt", "2026-06-01T10:00:00");
        body.put("durationMinutes", "notAnInt");

        Map<String, Object> result = classService.createSession(enrollment.getId(), teacher, body);

        assertNotNull(result);
    }

    @Test
    void createSessionZeroDurationDefaultsSixtyTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .build();

        ClassSession savedSession = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .durationMinutes(60)
                .status("scheduled")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(savedSession);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("scheduledAt", "2026-06-01T10:00:00");
        body.put("durationMinutes", "0");

        Map<String, Object> result = classService.createSession(enrollment.getId(), teacher, body);

        assertNotNull(result);
    }

    // ─── createSession not teacher owner ──────────────────────────────────────

    @Test
    void createSessionNotTeacherOwnerThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.createSession(enrollment.getId(), student,
                        Map.of("scheduledAt", "2026-06-01T10:00:00")));
    }

    // ─── updateSession without scheduledAt key ────────────────────────────────

    @Test
    void updateSessionNoScheduledAtKeyDoesNotUpdateDateTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("scheduled")
                .build();

        ClassSession session = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .status("scheduled")
                .build();

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(session);
        when(sessionRepository.findByEnrollmentOrderByScheduledAtAsc(enrollment))
                .thenReturn(List.of(session));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        java.util.Map<String, String> body = new java.util.HashMap<>();
        // no scheduledAt key → should not update scheduledAt

        Map<String, Object> result = classService.updateSession(session.getId(), teacher, body);

        assertNotNull(result);
    }

    // ─── syncEnrollmentWithSessions no sessions → enrollment unchanged ─────────

    @Test
    void updateSessionNoSessionsDoesNotSetCompletedTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("accepted")
                .hoursPurchased(1)
                .hoursUsed(0)
                .build();

        ClassSession session = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .status("cancelled")
                .build();

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ClassSession.class))).thenReturn(session);
        when(sessionRepository.findByEnrollmentOrderByScheduledAtAsc(enrollment))
                .thenReturn(List.of());
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        classService.updateSession(session.getId(), teacher, Map.of("status", "cancelled"));

        verify(enrollmentRepository).save(any());
    }

    // ─── toEnrollmentMap with nextSession non-null ─────────────────────────────

    @Test
    void listClassesWithNextSessionIncludesSessionMapTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("scheduled")
                .hoursPurchased(1)
                .hoursUsed(0)
                .build();

        ClassSession nextSession = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                .durationMinutes(60)
                .status("scheduled")
                .build();

        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student))
                .thenReturn(List.of(enrollment));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.of(nextSession));

        List<Map<String, Object>> result = classService.listClasses(student);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).get("nextSession"));
    }

    // ─── notifyStudentsAboutPost with empty recipients ─────────────────────────

    @Test
    void createPostNoActiveStudentsSkipsNotificationsTest() {
        ClassEnrollment rejectedEnrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("rejected")
                .build();

        when(postRepository.save(any(ClassPost.class))).thenAnswer(invocation -> {
            ClassPost post = invocation.getArgument(0);
            post.setId(UUID.randomUUID());
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });
        when(enrollmentRepository.findByTeacherOrderByCreatedAtDesc(teacher))
                .thenReturn(List.of(rejectedEnrollment));

        classService.createPost(teacher, "Novedad", "Texto");

        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }

    // ─── ensureChatEnabled refunded state ─────────────────────────────────────

    @Test
    void getMessagesRefundedStateThrowsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("refunded")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> classService.getMessages(enrollment.getId(), student));
    }

    // ─── toEnrollmentMap hoursRemaining calculation ───────────────────────────

    @Test
    void listClassesHoursRemainingNeverNegativeTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("completed")
                .hoursPurchased(1)
                .hoursUsed(2)
                .build();

        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student))
                .thenReturn(List.of(enrollment));
        when(sessionRepository.findFirstByEnrollmentAndStatusOrderByScheduledAtAsc(any(), any()))
                .thenReturn(Optional.empty());

        List<Map<String, Object>> result = classService.listClasses(student);

        assertEquals(0, result.get(0).get("hoursRemaining"));
    }

    // ─── updateStatus cancelled by teacher (actor is teacher) ─────────────────

    @Test
    void updateStatusCancelledByTeacherNotifiesStudentTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        classService.updateStatus(enrollment.getId(), teacher, Map.of("status", "cancelled"));

        verify(notificationService).create(eq(student), eq("class_request_cancelled"), any(), any(), any());
    }

    // ─── updateStatus rejected sends no notification to teacher ──────────────

    @Test
    void updateStatusRejectedNoReasonTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .teacherName(teacher.getName())
                .studentName(student.getName())
                .paymentStatus("pending")
                .build();

        when(enrollmentRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("status", "rejected");
        body.put("reason", null);

        classService.updateStatus(enrollment.getId(), teacher, body);

        verify(notificationService).create(eq(student), eq("class_request_rejected"), any(), any(), any());
    }

    // ─── isActiveRequestStatus covers scheduled/paid branches ─────────────────

    @Test
    void requestClassExistingScheduledRequestThrowsTest() {
        ClassEnrollment existingScheduled = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("scheduled")
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findFirstByTeacherAndStudentOrderByCreatedAtDesc(eq(teacher), eq(student)))
                .thenReturn(Optional.of(existingScheduled));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestClass(teacher.getId(), student, Map.of("message", "Hola")));
    }

    @Test
    void requestClassExistingPaidRequestThrowsTest() {
        ClassEnrollment existingPaid = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("paid")
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findFirstByTeacherAndStudentOrderByCreatedAtDesc(eq(teacher), eq(student)))
                .thenReturn(Optional.of(existingPaid));

        assertThrows(IllegalArgumentException.class,
                () -> classService.requestClass(teacher.getId(), student, Map.of("message", "Hola")));
    }

    @Test
    void requestClassRejectedStatusIsNotActiveAllowsNewRequestTest() {
        ClassEnrollment existingRejected = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .paymentStatus("rejected")
                .build();

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(enrollmentRepository.findFirstByTeacherAndStudentOrderByCreatedAtDesc(eq(teacher), eq(student)))
                .thenReturn(Optional.of(existingRejected));
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        Map<String, Object> result = classService.requestClass(teacher.getId(), student, Map.of("message", "Nueva solicitud"));

        assertEquals("pending", result.get("paymentStatus"));
    }
}
