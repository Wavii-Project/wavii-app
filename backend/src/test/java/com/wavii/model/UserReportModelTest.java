package com.wavii.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserReport model: Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor.
 */
class UserReportModelTest {

    private User buildUser(String email) {
        return User.builder().email(email).name("User " + email).build();
    }

    // ── Constructor & Builder ─────────────────────────────────────

    @Test
    void userReportNoArgsConstructorCreatesInstanceTest() {
        UserReport report = new UserReport();
        assertNotNull(report);
    }

    @Test
    void userReportBuilderSetsAllFieldsTest() {
        User reporter = buildUser("reporter@test.com");
        User reported = buildUser("reported@test.com");
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);

        UserReport report = UserReport.builder()
                .id(1L)
                .reporter(reporter)
                .reported(reported)
                .reason("spam")
                .createdAt(now)
                .build();

        assertEquals(1L, report.getId());
        assertEquals(reporter, report.getReporter());
        assertEquals(reported, report.getReported());
        assertEquals("spam", report.getReason());
        assertEquals(now, report.getCreatedAt());
    }

    @Test
    void userReportAllArgsConstructorTest() {
        User reporter = buildUser("r1@t.com");
        User reported = buildUser("r2@t.com");
        LocalDateTime now = LocalDateTime.now();

        UserReport report = new UserReport(42L, reporter, reported, "acoso", now);

        assertEquals(42L, report.getId());
        assertEquals(reporter, report.getReporter());
        assertEquals(reported, report.getReported());
        assertEquals("acoso", report.getReason());
        assertEquals(now, report.getCreatedAt());
    }

    // ── Builder defaults ──────────────────────────────────────────

    @Test
    void userReportBuilderDefaultCreatedAtIsNotNullTest() {
        UserReport report = UserReport.builder()
                .reason("spam")
                .build();

        assertNotNull(report.getCreatedAt());
    }

    // ── Setters / Getters (via @Data) ─────────────────────────────

    @Test
    void userReportSettersAndGettersWorkTest() {
        UserReport report = new UserReport();
        User reporter = buildUser("rep@test.com");
        User reported = buildUser("user@test.com");

        report.setId(5L);
        report.setReporter(reporter);
        report.setReported(reported);
        report.setReason("contenido inapropiado");

        assertEquals(5L, report.getId());
        assertEquals(reporter, report.getReporter());
        assertEquals(reported, report.getReported());
        assertEquals("contenido inapropiado", report.getReason());
    }

    // ── equals, hashCode, toString ────────────────────────────────

    @Test
    void userReportEqualsSameObjectTest() {
        UserReport report = UserReport.builder().reason("spam").build();
        assertEquals(report, report);
    }

    @Test
    void userReportEqualsEqualObjectsTest() {
        User reporter = buildUser("r@t.com");
        User reported = buildUser("u@t.com");
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        UserReport r1 = new UserReport(1L, reporter, reported, "spam", now);
        UserReport r2 = new UserReport(1L, reporter, reported, "spam", now);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void userReportEqualsDifferentReasonTest() {
        UserReport r1 = UserReport.builder().reason("spam").build();
        UserReport r2 = UserReport.builder().reason("acoso").build();
        assertNotEquals(r1, r2);
    }

    @Test
    void userReportEqualsNullTest() {
        UserReport report = UserReport.builder().reason("spam").build();
        assertNotEquals(report, null);
    }

    @Test
    void userReportEqualsDifferentTypeTest() {
        UserReport report = UserReport.builder().reason("spam").build();
        assertNotEquals(report, "string");
    }

    @Test
    void userReportHashCodeConsistentTest() {
        UserReport report = UserReport.builder().reason("spam").build();
        assertEquals(report.hashCode(), report.hashCode());
    }

    @Test
    void userReportToStringNotNullTest() {
        UserReport report = UserReport.builder()
                .reason("spam")
                .build();
        assertNotNull(report.toString());
        assertTrue(report.toString().contains("UserReport") || report.toString().contains("spam"));
    }

    @Test
    void userReportNullReasonAllowedTest() {
        UserReport report = UserReport.builder()
                .reason(null)
                .build();

        assertNull(report.getReason());
    }
}
