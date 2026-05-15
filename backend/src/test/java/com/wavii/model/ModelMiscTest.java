package com.wavii.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for model entities not covered by other test classes.
 */
class ModelMiscTest {

    // ── ForumLike ─────────────────────────────────────────────────

    @Test
    void forumLikeOnCreateSetsLikedAtTest() {
        ForumLike like = new ForumLike();
        assertNull(like.getLikedAt());
        like.onCreate();
        assertNotNull(like.getLikedAt());
    }

    @Test
    void forumLikeBuilderSetsFieldsTest() {
        Forum forum = Forum.builder().name("Jazz").build();
        User user = User.builder().email("a@b.com").name("Ana").build();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        ForumLike like = ForumLike.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .user(user)
                .likedAt(now)
                .build();
        assertEquals(forum, like.getForum());
        assertEquals(user, like.getUser());
        assertEquals(now, like.getLikedAt());
    }

    @Test
    void forumLikeNoArgConstructorTest() {
        ForumLike like = new ForumLike();
        assertNull(like.getId());
        assertNull(like.getForum());
    }

    @Test
    void forumLikeAllArgConstructorTest() {
        UUID id = UUID.randomUUID();
        Forum forum = Forum.builder().name("Jazz").build();
        User user = User.builder().email("a@b.com").name("Ana").build();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        ForumLike like = new ForumLike(id, forum, user, now);
        assertEquals(id, like.getId());
        assertEquals(forum, like.getForum());
        assertEquals(user, like.getUser());
        assertEquals(now, like.getLikedAt());
    }

    // ── ClassMessage ──────────────────────────────────────────────

    @Test
    void classMessageBuilderSetsFieldsTest() {
        User sender = User.builder().email("a@b.com").name("Ana").build();
        ClassMessage msg = ClassMessage.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .content("Hello")
                .build();
        assertEquals(sender, msg.getSender());
        assertEquals("Hello", msg.getContent());
    }

    @Test
    void classMessageNoArgConstructorTest() {
        ClassMessage msg = new ClassMessage();
        assertNull(msg.getId());
        assertNull(msg.getContent());
    }

    @Test
    void classMessageCreatedAtDefaultTest() {
        ClassMessage msg = ClassMessage.builder()
                .content("Test")
                .build();
        assertNotNull(msg.getCreatedAt());
    }

    @Test
    void classMessageSettersTest() {
        ClassMessage msg = new ClassMessage();
        UUID id = UUID.randomUUID();
        msg.setId(id);
        msg.setContent("Hello");
        assertEquals(id, msg.getId());
        assertEquals("Hello", msg.getContent());
    }

    // ── AppNotification ───────────────────────────────────────────

    @Test
    void appNotificationBuilderSetsFieldsTest() {
        User recipient = User.builder().email("a@b.com").name("Ana").build();
        AppNotification notif = AppNotification.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .type("LIKE")
                .title("You got a like")
                .body("Someone liked your post")
                .dataJson("{\"id\":1}")
                .build();
        assertEquals(recipient, notif.getRecipient());
        assertEquals("LIKE", notif.getType());
        assertEquals("You got a like", notif.getTitle());
        assertFalse(notif.isRead());
        assertNotNull(notif.getCreatedAt());
    }

    @Test
    void appNotificationNoArgConstructorTest() {
        AppNotification notif = new AppNotification();
        assertNull(notif.getId());
        assertNull(notif.getType());
    }

    @Test
    void appNotificationReadDefaultFalseTest() {
        AppNotification notif = AppNotification.builder()
                .type("MSG")
                .title("Title")
                .body("Body")
                .build();
        assertFalse(notif.isRead());
    }

    @Test
    void appNotificationSetReadTest() {
        AppNotification notif = new AppNotification();
        notif.setRead(true);
        assertTrue(notif.isRead());
    }

    @Test
    void appNotificationSettersTest() {
        AppNotification notif = new AppNotification();
        notif.setType("NEW");
        notif.setTitle("T");
        notif.setBody("B");
        notif.setDataJson("{\"x\":1}");
        assertEquals("NEW", notif.getType());
        assertEquals("T", notif.getTitle());
        assertEquals("B", notif.getBody());
        assertEquals("{\"x\":1}", notif.getDataJson());
    }

    // ── ClassEnrollment ───────────────────────────────────────────

    @Test
    void classEnrollmentBuilderSetsFieldsTest() {
        User teacher = User.builder().email("t@t.com").name("Teacher").build();
        User student = User.builder().email("s@s.com").name("Student").build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .student(student)
                .instrument("Piano")
                .city("Madrid")
                .paymentStatus("pending")
                .build();
        assertEquals(teacher, enrollment.getTeacher());
        assertEquals(student, enrollment.getStudent());
        assertEquals("Piano", enrollment.getInstrument());
        assertEquals("Madrid", enrollment.getCity());
        assertEquals("pending", enrollment.getPaymentStatus());
        assertEquals(1, enrollment.getHoursPurchased());
        assertEquals(0, enrollment.getHoursUsed());
    }

    @Test
    void classEnrollmentNoArgConstructorTest() {
        ClassEnrollment enrollment = new ClassEnrollment();
        assertNull(enrollment.getId());
        assertNull(enrollment.getInstrument());
    }

    @Test
    void classEnrollmentSettersTest() {
        ClassEnrollment enrollment = new ClassEnrollment();
        UUID id = UUID.randomUUID();
        enrollment.setId(id);
        enrollment.setInstrument("Guitar");
        enrollment.setCity("Barcelona");
        enrollment.setPaymentStatus("accepted");
        enrollment.setHoursPurchased(3);
        enrollment.setHoursUsed(1);
        enrollment.setUnitPrice(new BigDecimal("30.00"));
        assertEquals(id, enrollment.getId());
        assertEquals("Guitar", enrollment.getInstrument());
        assertEquals("Barcelona", enrollment.getCity());
        assertEquals("accepted", enrollment.getPaymentStatus());
        assertEquals(3, enrollment.getHoursPurchased());
        assertEquals(1, enrollment.getHoursUsed());
        assertEquals(new BigDecimal("30.00"), enrollment.getUnitPrice());
    }

    @Test
    void classEnrollmentCreatedAtDefaultTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .instrument("Guitar")
                .build();
        assertNotNull(enrollment.getCreatedAt());
    }

    // ── ClassSession ──────────────────────────────────────────────

    @Test
    void classSessionOnUpdateSetsUpdatedAtTest() throws InterruptedException {
        ClassSession session = new ClassSession();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        session.onUpdate();
        assertNotNull(session.getUpdatedAt());
        assertTrue(session.getUpdatedAt().isAfter(before));
    }

    @Test
    void classSessionBuilderSetsFieldsTest() {
        ClassEnrollment enrollment = ClassEnrollment.builder().build();
        User teacher = User.builder().email("t@t.com").name("Teacher").build();
        User student = User.builder().email("s@s.com").name("Student").build();
        LocalDateTime scheduledAt = LocalDateTime.of(2024, 6, 1, 10, 0);
        ClassSession session = ClassSession.builder()
                .id(UUID.randomUUID())
                .enrollment(enrollment)
                .teacher(teacher)
                .student(student)
                .scheduledAt(scheduledAt)
                .meetingUrl("https://meet.example.com")
                .notes("Please be on time")
                .build();
        assertEquals(enrollment, session.getEnrollment());
        assertEquals(teacher, session.getTeacher());
        assertEquals(scheduledAt, session.getScheduledAt());
        assertEquals("https://meet.example.com", session.getMeetingUrl());
        assertEquals(60, session.getDurationMinutes());
        assertEquals("scheduled", session.getStatus());
    }

    @Test
    void classSessionNoArgConstructorTest() {
        ClassSession session = new ClassSession();
        assertNull(session.getId());
        assertNull(session.getScheduledAt());
    }

    @Test
    void classSessionSettersTest() {
        ClassSession session = new ClassSession();
        session.setStatus("completed");
        session.setDurationMinutes(90);
        session.setNotes("Notes");
        assertEquals("completed", session.getStatus());
        assertEquals(90, session.getDurationMinutes());
        assertEquals("Notes", session.getNotes());
    }

    // ── ClassPost ─────────────────────────────────────────────────

    @Test
    void classPostBuilderSetsFieldsTest() {
        User teacher = User.builder().email("t@t.com").name("Teacher").build();
        ClassPost post = ClassPost.builder()
                .id(UUID.randomUUID())
                .teacher(teacher)
                .title("Lesson 1")
                .content("Introduction to scales")
                .build();
        assertEquals(teacher, post.getTeacher());
        assertEquals("Lesson 1", post.getTitle());
        assertEquals("Introduction to scales", post.getContent());
        assertNotNull(post.getCreatedAt());
    }

    @Test
    void classPostNoArgConstructorTest() {
        ClassPost post = new ClassPost();
        assertNull(post.getId());
        assertNull(post.getTitle());
    }

    @Test
    void classPostSettersTest() {
        ClassPost post = new ClassPost();
        post.setTitle("Lesson 2");
        post.setContent("Chord progressions");
        assertEquals("Lesson 2", post.getTitle());
        assertEquals("Chord progressions", post.getContent());
    }

    // ── PdfReport ─────────────────────────────────────────────────

    @Test
    void pdfReportBuilderSetsFieldsTest() {
        PdfDocument pdf = PdfDocument.builder().fileName("test.pdf").build();
        User reporter = User.builder().email("a@b.com").name("Ana").build();
        PdfReport report = PdfReport.builder()
                .pdfDocument(pdf)
                .reporter(reporter)
                .reason("Inappropriate")
                .details("Contains harmful content")
                .build();
        assertEquals(pdf, report.getPdfDocument());
        assertEquals(reporter, report.getReporter());
        assertEquals("Inappropriate", report.getReason());
        assertEquals("Contains harmful content", report.getDetails());
        assertEquals("PENDING", report.getStatus());
        assertNotNull(report.getCreatedAt());
    }

    @Test
    void pdfReportNoArgConstructorTest() {
        PdfReport report = new PdfReport();
        assertNull(report.getId());
        assertNull(report.getReason());
    }

    @Test
    void pdfReportStatusDefaultPendingTest() {
        PdfReport report = PdfReport.builder()
                .reason("Spam")
                .build();
        assertEquals("PENDING", report.getStatus());
    }

    @Test
    void pdfReportSettersTest() {
        PdfReport report = new PdfReport();
        report.setReason("Spam");
        report.setDetails("Details");
        report.setStatus("RESOLVED");
        assertEquals("Spam", report.getReason());
        assertEquals("Details", report.getDetails());
        assertEquals("RESOLVED", report.getStatus());
    }
}
