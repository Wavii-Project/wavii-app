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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassControllerTest {

    @Mock
    private ClassService classService;

    @InjectMocks
    private ClassController classController;

    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(UUID.randomUUID());
        teacher.setName("Prof. Ana");
        teacher.setEmail("ana@wavii.app");
        teacher.setRole(Role.PROFESOR_CERTIFICADO);

        student = new User();
        student.setId(UUID.randomUUID());
        student.setName("Alumno");
        student.setEmail("alumno@wavii.app");
        student.setRole(Role.USUARIO);
    }

    @Test
    void listClassesNullUserReturnsUnauthorizedTest() {
        ResponseEntity<?> result = classController.listClasses(null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void listClassesValidUserReturnsOkTest() {
        when(classService.listClasses(student)).thenReturn(List.of());

        ResponseEntity<?> result = classController.listClasses(student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(classService).listClasses(student);
    }

    @Test
    void listClassesServiceThrowsReturnsBadRequestTest() {
        when(classService.listClasses(student)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.listClasses(student);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void manageNullUserReturnsUnauthorizedTest() {
        ResponseEntity<?> result = classController.manage(null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void manageValidTeacherReturnsOkTest() {
        when(classService.getManageOverview(teacher)).thenReturn(Map.of("classes", List.of()));

        ResponseEntity<?> result = classController.manage(teacher);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(classService).getManageOverview(teacher);
    }

    @Test
    void manageServiceThrowsReturnsBadRequestTest() {
        when(classService.getManageOverview(teacher)).thenThrow(new RuntimeException("Solo un profesor"));

        ResponseEntity<?> result = classController.manage(teacher);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void checkoutSuccessReturnsOkTest() throws Exception {
        UUID teacherId = teacher.getId();
        when(classService.checkout(teacherId, student)).thenReturn(Map.of("enrollmentId", UUID.randomUUID().toString()));

        ResponseEntity<?> result = classController.checkout(teacherId, student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(classService).checkout(teacherId, student);
    }

    @Test
    void checkoutServiceThrowsReturnsBadRequestTest() throws Exception {
        UUID teacherId = teacher.getId();
        when(classService.checkout(teacherId, student)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.checkout(teacherId, student);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void requestClassSuccessReturnsOkTest() {
        UUID teacherId = teacher.getId();
        Map<String, String> body = Map.of("message", "Hola");
        when(classService.requestClass(teacherId, student, body)).thenReturn(Map.of("paymentStatus", "pending"));

        ResponseEntity<?> result = classController.requestClass(teacherId, student, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(classService).requestClass(teacherId, student, body);
    }

    @Test
    void requestClassServiceThrowsReturnsBadRequestTest() {
        UUID teacherId = teacher.getId();
        Map<String, String> body = Map.of("message", "Hola");
        when(classService.requestClass(teacherId, student, body)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.requestClass(teacherId, student, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void confirmSuccessReturnsOkTest() {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.confirm(enrollmentId, student)).thenReturn(Map.of("paymentStatus", "paid"));

        ResponseEntity<?> result = classController.confirm(enrollmentId, student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void confirmServiceThrowsReturnsBadRequestTest() {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.confirm(enrollmentId, student)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.confirm(enrollmentId, student);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void messagesSuccessReturnsOkTest() {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.getMessages(enrollmentId, student)).thenReturn(List.of());

        ResponseEntity<?> result = classController.messages(enrollmentId, student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void messagesServiceThrowsReturnsForbiddenTest() {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.getMessages(enrollmentId, student)).thenThrow(new RuntimeException("Forbidden"));

        ResponseEntity<?> result = classController.messages(enrollmentId, student);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void sendMessageSuccessReturnsOkTest() {
        UUID enrollmentId = UUID.randomUUID();
        Map<String, String> body = Map.of("content", "Hola");
        when(classService.sendMessage(enrollmentId, student, "Hola")).thenReturn(Map.of("id", UUID.randomUUID().toString()));

        ResponseEntity<?> result = classController.sendMessage(enrollmentId, student, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void sendMessageServiceThrowsReturnsBadRequestTest() {
        UUID enrollmentId = UUID.randomUUID();
        Map<String, String> body = Map.of("content", "");
        when(classService.sendMessage(enrollmentId, student, "")).thenThrow(new RuntimeException("Vacio"));

        ResponseEntity<?> result = classController.sendMessage(enrollmentId, student, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void postsSuccessReturnsOkTest() {
        UUID teacherId = teacher.getId();
        when(classService.getPostsForViewer(student, teacherId)).thenReturn(List.of());

        ResponseEntity<?> result = classController.posts(teacherId, student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void postsServiceThrowsReturnsForbiddenTest() {
        UUID teacherId = teacher.getId();
        when(classService.getPostsForViewer(student, teacherId)).thenThrow(new RuntimeException("Forbidden"));

        ResponseEntity<?> result = classController.posts(teacherId, student);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void studentPostsSuccessReturnsOkTest() {
        when(classService.getPostsForStudent(student)).thenReturn(List.of());

        ResponseEntity<?> result = classController.studentPosts(student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void studentPostsServiceThrowsReturnsForbiddenTest() {
        when(classService.getPostsForStudent(student)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.studentPosts(student);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void createPostSameUserAllowedReturnsOkTest() {
        UUID teacherId = teacher.getId();
        Map<String, String> body = Map.of("title", "Titulo", "content", "Contenido");
        when(classService.createPost(teacher, "Titulo", "Contenido")).thenReturn(Map.of("title", "Titulo"));

        ResponseEntity<?> result = classController.createPost(teacherId, teacher, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void createPostDifferentUserReturnsForbiddenTest() {
        UUID teacherId = teacher.getId();
        Map<String, String> body = Map.of("title", "Titulo", "content", "Contenido");

        ResponseEntity<?> result = classController.createPost(teacherId, student, body);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        verifyNoInteractions(classService);
    }

    @Test
    void createPostNullUserReturnsForbiddenTest() {
        UUID teacherId = teacher.getId();
        Map<String, String> body = Map.of("title", "Titulo", "content", "Contenido");

        ResponseEntity<?> result = classController.createPost(teacherId, null, body);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void requestExtraHourSuccessReturnsOkTest() throws Exception {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.requestExtraHour(enrollmentId, student)).thenReturn(Map.of("enrollmentId", enrollmentId.toString()));

        ResponseEntity<?> result = classController.requestExtraHour(enrollmentId, student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void requestExtraHourServiceThrowsReturnsBadRequestTest() throws Exception {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.requestExtraHour(enrollmentId, student)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.requestExtraHour(enrollmentId, student);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void requestRefundSuccessReturnsOkTest() {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.requestRefund(enrollmentId, student)).thenReturn(Map.of("paymentStatus", "refunded"));

        ResponseEntity<?> result = classController.requestRefund(enrollmentId, student);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void requestRefundServiceThrowsReturnsBadRequestTest() {
        UUID enrollmentId = UUID.randomUUID();
        when(classService.requestRefund(enrollmentId, student)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.requestRefund(enrollmentId, student);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void updateStatusSuccessReturnsOkTest() {
        UUID enrollmentId = UUID.randomUUID();
        Map<String, String> body = Map.of("status", "accepted");
        when(classService.updateStatus(enrollmentId, teacher, body)).thenReturn(Map.of("paymentStatus", "accepted"));

        ResponseEntity<?> result = classController.updateStatus(enrollmentId, teacher, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateStatusServiceThrowsReturnsBadRequestTest() {
        UUID enrollmentId = UUID.randomUUID();
        Map<String, String> body = Map.of("status", "invalid");
        when(classService.updateStatus(enrollmentId, teacher, body)).thenThrow(new RuntimeException("Estado no valido"));

        ResponseEntity<?> result = classController.updateStatus(enrollmentId, teacher, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void createSessionSuccessReturnsOkTest() {
        UUID enrollmentId = UUID.randomUUID();
        Map<String, String> body = Map.of("scheduledAt", "2026-06-01T10:00:00");
        when(classService.createSession(enrollmentId, teacher, body)).thenReturn(Map.of("id", UUID.randomUUID().toString()));

        ResponseEntity<?> result = classController.createSession(enrollmentId, teacher, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void createSessionServiceThrowsReturnsBadRequestTest() {
        UUID enrollmentId = UUID.randomUUID();
        Map<String, String> body = Map.of();
        when(classService.createSession(enrollmentId, teacher, body)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.createSession(enrollmentId, teacher, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void updateSessionSuccessReturnsOkTest() {
        UUID sessionId = UUID.randomUUID();
        Map<String, String> body = Map.of("status", "completed");
        when(classService.updateSession(sessionId, teacher, body)).thenReturn(Map.of("status", "completed"));

        ResponseEntity<?> result = classController.updateSession(sessionId, teacher, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateSessionServiceThrowsReturnsBadRequestTest() {
        UUID sessionId = UUID.randomUUID();
        Map<String, String> body = Map.of("status", "invalid");
        when(classService.updateSession(sessionId, teacher, body)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = classController.updateSession(sessionId, teacher, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}
