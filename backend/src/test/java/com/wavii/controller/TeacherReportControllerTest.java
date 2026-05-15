package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.model.enums.Role;
import com.wavii.service.ClassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherReportControllerTest {

    @Mock
    private ClassService classService;

    @InjectMocks
    private TeacherReportController teacherReportController;

    private User reporter;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        reporter = new User();
        reporter.setId(UUID.randomUUID());
        reporter.setName("Reporter");
        reporter.setEmail("reporter@wavii.app");
        reporter.setRole(Role.USUARIO);

        teacherId = UUID.randomUUID();
    }

    @Test
    void reportTeacherNullUserReturnsUnauthorizedTest() {
        Map<String, String> body = Map.of("reason", "Comportamiento inadecuado");

        ResponseEntity<?> result = teacherReportController.reportTeacher(teacherId, null, body);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        verifyNoInteractions(classService);
    }

    @Test
    void reportTeacherSuccessReturnsOkTest() {
        Map<String, String> body = Map.of("reason", "Comportamiento inadecuado");
        when(classService.createTeacherReport(eq(teacherId), eq(reporter), eq(body)))
                .thenReturn(Map.of("id", UUID.randomUUID().toString(), "status", "pending"));

        ResponseEntity<?> result = teacherReportController.reportTeacher(teacherId, reporter, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) result.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.containsKey("id"));
        verify(classService).createTeacherReport(teacherId, reporter, body);
    }

    @Test
    void reportTeacherServiceThrowsReturnsBadRequestTest() {
        Map<String, String> body = Map.of("reason", "Motivo");
        when(classService.createTeacherReport(eq(teacherId), eq(reporter), eq(body)))
                .thenThrow(new IllegalArgumentException("Profesor no encontrado"));

        ResponseEntity<?> result = teacherReportController.reportTeacher(teacherId, reporter, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) result.getBody();
        assertNotNull(responseBody);
        assertEquals("Profesor no encontrado", responseBody.get("message"));
    }

    @Test
    void reportTeacherSelfReportThrowsReturnsBadRequestTest() {
        Map<String, String> body = Map.of("reason", "Intento de auto-reporte");
        when(classService.createTeacherReport(eq(teacherId), eq(reporter), any()))
                .thenThrow(new IllegalArgumentException("No puedes reportarte a ti mismo"));

        ResponseEntity<?> result = teacherReportController.reportTeacher(teacherId, reporter, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void reportTeacherMissingReasonReturnsBadRequestTest() {
        Map<String, String> body = Map.of();
        when(classService.createTeacherReport(eq(teacherId), eq(reporter), eq(body)))
                .thenThrow(new IllegalArgumentException("Debes indicar un motivo"));

        ResponseEntity<?> result = teacherReportController.reportTeacher(teacherId, reporter, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}
