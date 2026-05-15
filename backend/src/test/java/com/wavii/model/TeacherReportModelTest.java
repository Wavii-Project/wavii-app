package com.wavii.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TeacherReport model: Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor.
 */
class TeacherReportModelTest {

    private User buildUser(String email) {
        return User.builder().email(email).name("User " + email).build();
    }

    // ── Constructor & Builder ─────────────────────────────────────

    @Test
    void teacherReportNoArgsConstructorCreatesInstanceTest() {
        TeacherReport report = new TeacherReport();
        assertNotNull(report);
    }

    @Test
    void teacherReportBuilderSetsAllFieldsTest() {
        UUID id = UUID.randomUUID();
        User teacher = buildUser("teacher@test.com");
        User reporter = buildUser("reporter@test.com");
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);

        TeacherReport report = TeacherReport.builder()
                .id(id)
                .teacher(teacher)
                .reporter(reporter)
                .reason("Comportamiento inapropiado")
                .details("El profesor no se presentó a la clase")
                .status("open")
                .createdAt(now)
                .build();

        assertEquals(id, report.getId());
        assertEquals(teacher, report.getTeacher());
        assertEquals(reporter, report.getReporter());
        assertEquals("Comportamiento inapropiado", report.getReason());
        assertEquals("El profesor no se presentó a la clase", report.getDetails());
        assertEquals("open", report.getStatus());
        assertEquals(now, report.getCreatedAt());
    }

    @Test
    void teacherReportAllArgsConstructorTest() {
        UUID id = UUID.randomUUID();
        User teacher = buildUser("t@t.com");
        User reporter = buildUser("r@r.com");
        LocalDateTime now = LocalDateTime.now();

        TeacherReport report = new TeacherReport(id, teacher, reporter,
                "Fraude", "Detalles del fraude", "open", now);

        assertEquals(id, report.getId());
        assertEquals(teacher, report.getTeacher());
        assertEquals(reporter, report.getReporter());
        assertEquals("Fraude", report.getReason());
        assertEquals("Detalles del fraude", report.getDetails());
        assertEquals("open", report.getStatus());
        assertEquals(now, report.getCreatedAt());
    }

    // ── Builder defaults ──────────────────────────────────────────

    @Test
    void teacherReportBuilderDefaultStatusIsOpenTest() {
        TeacherReport report = TeacherReport.builder()
                .reason("Test reason")
                .build();

        assertEquals("open", report.getStatus());
    }

    @Test
    void teacherReportBuilderDefaultCreatedAtIsNotNullTest() {
        TeacherReport report = TeacherReport.builder()
                .reason("Test reason")
                .build();

        assertNotNull(report.getCreatedAt());
    }

    // ── Setters / Getters (via @Data) ─────────────────────────────

    @Test
    void teacherReportSettersAndGettersWorkTest() {
        TeacherReport report = new TeacherReport();
        UUID id = UUID.randomUUID();
        User teacher = buildUser("t@t.com");
        User reporter = buildUser("r@r.com");

        report.setId(id);
        report.setTeacher(teacher);
        report.setReporter(reporter);
        report.setReason("Abuso");
        report.setDetails("Detalles");
        report.setStatus("closed");

        assertEquals(id, report.getId());
        assertEquals(teacher, report.getTeacher());
        assertEquals(reporter, report.getReporter());
        assertEquals("Abuso", report.getReason());
        assertEquals("Detalles", report.getDetails());
        assertEquals("closed", report.getStatus());
    }

    // ── equals, hashCode, toString ────────────────────────────────

    @Test
    void teacherReportEqualsSameObjectTest() {
        TeacherReport report = TeacherReport.builder().reason("reason").build();
        assertEquals(report, report);
    }

    @Test
    void teacherReportEqualsEqualObjectsTest() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        User teacher = buildUser("t@t.com");
        User reporter = buildUser("r@r.com");

        TeacherReport r1 = new TeacherReport(id, teacher, reporter, "Reason", "Details", "open", now);
        TeacherReport r2 = new TeacherReport(id, teacher, reporter, "Reason", "Details", "open", now);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void teacherReportEqualsDifferentReasonTest() {
        TeacherReport r1 = TeacherReport.builder().reason("Reason A").build();
        TeacherReport r2 = TeacherReport.builder().reason("Reason B").build();
        assertNotEquals(r1, r2);
    }

    @Test
    void teacherReportEqualsNullTest() {
        TeacherReport report = TeacherReport.builder().reason("reason").build();
        assertNotEquals(report, null);
    }

    @Test
    void teacherReportEqualsDifferentTypeTest() {
        TeacherReport report = TeacherReport.builder().reason("reason").build();
        assertNotEquals(report, "string");
    }

    @Test
    void teacherReportHashCodeConsistentTest() {
        TeacherReport report = TeacherReport.builder().reason("reason").build();
        assertEquals(report.hashCode(), report.hashCode());
    }

    @Test
    void teacherReportToStringNotNullTest() {
        TeacherReport report = TeacherReport.builder()
                .reason("Comportamiento inapropiado")
                .status("open")
                .build();
        assertNotNull(report.toString());
        assertTrue(report.toString().contains("TeacherReport") || report.toString().contains("open"));
    }

    @Test
    void teacherReportNullDetailsAllowedTest() {
        TeacherReport report = TeacherReport.builder()
                .reason("reason")
                .details(null)
                .build();

        assertNull(report.getDetails());
    }
}
