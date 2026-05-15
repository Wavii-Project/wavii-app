package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.service.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ResponseEntity<?> listClasses(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Sesion no valida"));
        }
        try {
            return ResponseEntity.ok(classService.listClasses(currentUser));
        } catch (Exception e) {
            log.warn("Error cargando clases: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "No se pudieron cargar las clases"));
        }
    }

    @GetMapping("/manage")
    public ResponseEntity<?> manage(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Sesion no valida"));
        }
        try {
            return ResponseEntity.ok(classService.getManageOverview(currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{teacherId}/checkout")
    public ResponseEntity<?> checkout(@PathVariable UUID teacherId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.checkout(teacherId, currentUser));
        } catch (Exception e) {
            log.warn("Error preparando pago de clase: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{teacherId}/request")
    public ResponseEntity<?> requestClass(@PathVariable UUID teacherId,
                                          @AuthenticationPrincipal User currentUser,
                                          @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(classService.requestClass(teacherId, currentUser, body));
        } catch (Exception e) {
            log.warn("Error creando solicitud de clase: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{enrollmentId}/confirm")
    public ResponseEntity<?> confirm(@PathVariable UUID enrollmentId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.confirm(enrollmentId, currentUser));
        } catch (Exception e) {
            log.warn("Error confirmando clase: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{enrollmentId}/messages")
    public ResponseEntity<?> messages(@PathVariable UUID enrollmentId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.getMessages(enrollmentId, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{enrollmentId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable UUID enrollmentId,
                                         @AuthenticationPrincipal User currentUser,
                                         @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(classService.sendMessage(enrollmentId, currentUser, body.get("content")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{teacherId}/posts")
    public ResponseEntity<?> posts(@PathVariable UUID teacherId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.getPostsForViewer(currentUser, teacherId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/posts")
    public ResponseEntity<?> studentPosts(@AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.getPostsForStudent(currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{teacherId}/posts")
    public ResponseEntity<?> createPost(@PathVariable UUID teacherId,
                                        @AuthenticationPrincipal User currentUser,
                                        @RequestBody Map<String, String> body) {
        try {
            if (currentUser == null || !currentUser.getId().equals(teacherId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Solo puedes publicar en tus propias clases"));
            }
            return ResponseEntity.ok(classService.createPost(currentUser, body.get("title"), body.get("content")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{enrollmentId}/request-extra-hour")
    public ResponseEntity<?> requestExtraHour(@PathVariable UUID enrollmentId,
                                              @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.requestExtraHour(enrollmentId, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{enrollmentId}/refund-request")
    public ResponseEntity<?> requestRefund(@PathVariable UUID enrollmentId,
                                           @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.requestRefund(enrollmentId, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{enrollmentId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID enrollmentId,
                                          @AuthenticationPrincipal User currentUser,
                                          @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(classService.updateStatus(enrollmentId, currentUser, body));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{enrollmentId}/sessions")
    public ResponseEntity<?> createSession(@PathVariable UUID enrollmentId,
                                           @AuthenticationPrincipal User currentUser,
                                           @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(classService.createSession(enrollmentId, currentUser, body));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/sessions/{sessionId}")
    public ResponseEntity<?> updateSession(@PathVariable UUID sessionId,
                                           @AuthenticationPrincipal User currentUser,
                                           @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(classService.updateSession(sessionId, currentUser, body));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}
