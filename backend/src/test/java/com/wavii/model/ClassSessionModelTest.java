package com.wavii.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de cobertura para la entidad ClassSession.
 * Cubre getters, setters, builder, equals, hashCode, toString y onUpdate.
 * 
 * @author eduglezexp
 */
class ClassSessionModelTest {

    @Test
    void gettersAndSettersWorkCorrectlyTest() {
        ClassSession session = new ClassSession();
        UUID id = UUID.randomUUID();
        ClassEnrollment enrollment = new ClassEnrollment();
        User teacher = new User();
        User student = new User();
        LocalDateTime sched = LocalDateTime.now().plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        session.setId(id);
        session.setEnrollment(enrollment);
        session.setTeacher(teacher);
        session.setStudent(student);
        session.setScheduledAt(sched);
        session.setDurationMinutes(90);
        session.setStatus("completed");
        session.setMeetingUrl("http://meet.com");
        session.setNotes("Some notes");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        assertEquals(id, session.getId());
        assertEquals(enrollment, session.getEnrollment());
        assertEquals(teacher, session.getTeacher());
        assertEquals(student, session.getStudent());
        assertEquals(sched, session.getScheduledAt());
        assertEquals(90, session.getDurationMinutes());
        assertEquals("completed", session.getStatus());
        assertEquals("http://meet.com", session.getMeetingUrl());
        assertEquals("Some notes", session.getNotes());
        assertEquals(now, session.getCreatedAt());
        assertEquals(now, session.getUpdatedAt());
    }

    @Test
    void builderAndDefaultsTest() {
        ClassSession session = ClassSession.builder()
                .status("scheduled")
                .build();

        assertEquals(60, session.getDurationMinutes());
        assertEquals("scheduled", session.getStatus());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getUpdatedAt());
    }

    @Test
    void onUpdateMethodTest() throws InterruptedException {
        ClassSession session = new ClassSession();
        LocalDateTime before = session.getUpdatedAt();
        // Forzar una pequeña espera para asegurar cambio de tiempo si se usa LocalDateTime.now()
        Thread.sleep(10);
        session.onUpdate();
        assertNotEquals(before, session.getUpdatedAt());
    }

    @Test
    void allArgsConstructorTest() {
        UUID id = UUID.randomUUID();
        ClassEnrollment e = new ClassEnrollment();
        User t = new User();
        User s = new User();
        LocalDateTime now = LocalDateTime.now();

        ClassSession session = new ClassSession(
                id, e, t, s, now, 45, "cancelled", "url", "notes", now, now
        );

        assertEquals(id, session.getId());
        assertEquals(45, session.getDurationMinutes());
        assertEquals("cancelled", session.getStatus());
    }

    @Test
    void equalsHashCodeToStringExhaustiveTest() throws Exception {
        UUID id = UUID.randomUUID();
        ClassSession s1 = ClassSession.builder()
                .id(id)
                .status("S1")
                .durationMinutes(60)
                .scheduledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        ClassSession s2 = ClassSession.builder()
                .id(id)
                .status("S1")
                .durationMinutes(60)
                .scheduledAt(s1.getScheduledAt())
                .createdAt(s1.getCreatedAt())
                .build();

        ModelTestHelper.testEqualsAndHashCodeExhaustively(s1, s2, ClassSession.class);
    }
    @Test
    void classSessionBuilderCoverageTest() {
        ClassSession.builder()
            .id(UUID.randomUUID())
            .enrollment(new ClassEnrollment())
            .teacher(new User())
            .student(new User())
            .scheduledAt(LocalDateTime.now())
            .durationMinutes(30)
            .status("done")
            .meetingUrl("u")
            .notes("n")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
