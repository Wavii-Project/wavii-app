package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.service.ChatRealtimeBroadcaster;
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
    private final ChatRealtimeBroadcaster chatRealtimeBroadcaster;

    /**
     * Lista las clases en las que participa el usuario actual.
     * 
     * @param currentUser Usuario autenticado.
     * @return Listado de clases.
     */
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

    /**
     * Obtiene un resumen de gestión para el profesor o alumno.
     * 
     * @param currentUser Usuario autenticado.
     * @return Resumen de clases y solicitudes.
     */
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

    /**
     * Inicia el proceso de pago para contratar a un profesor.
     * 
     * @param teacherId ID del profesor a contratar.
     * @param currentUser Usuario autenticado (alumno).
     * @return Datos para el checkout de Stripe.
     */
    @PostMapping("/{teacherId}/checkout")
    public ResponseEntity<?> checkout(@PathVariable UUID teacherId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.checkout(teacherId, currentUser));
        } catch (Exception e) {
            log.warn("Error preparando pago de clase: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Crea una solicitud de clase (tras el pago o reserva).
     * 
     * @param teacherId ID del profesor.
     * @param currentUser Usuario autenticado.
     * @param body Datos de la solicitud.
     * @return La inscripción creada.
     */
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

    /**
     * Confirma una inscripción de clase.
     * 
     * @param enrollmentId ID de la inscripción.
     * @param currentUser Usuario autenticado.
     * @return La inscripción confirmada.
     */
    @PostMapping("/{enrollmentId}/confirm")
    public ResponseEntity<?> confirm(@PathVariable UUID enrollmentId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.confirm(enrollmentId, currentUser));
        } catch (Exception e) {
            log.warn("Error confirmando clase: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Obtiene los mensajes del chat de una clase.
     * 
     * @param enrollmentId ID de la inscripción.
     * @param currentUser Usuario autenticado.
     * @return Lista de mensajes.
     */
    @GetMapping("/{enrollmentId}/messages")
    public ResponseEntity<?> messages(@PathVariable UUID enrollmentId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.getMessages(enrollmentId, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Envía un mensaje al chat de la clase.
     * 
     * @param enrollmentId ID de la inscripción.
     * @param currentUser Usuario autenticado.
     * @param body Cuerpo con el contenido del mensaje.
     * @return El mensaje enviado.
     */
    @PostMapping("/{enrollmentId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable UUID enrollmentId,
                                         @AuthenticationPrincipal User currentUser,
                                         @RequestBody Map<String, String> body) {
        try {
            Map<String, Object> saved = classService.sendMessage(enrollmentId, currentUser, body.get("content"));
            chatRealtimeBroadcaster.broadcast(
                    chatRealtimeBroadcaster.classRoom(enrollmentId),
                    saved
            );
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Obtiene los posts (muro) de las clases de un profesor.
     * 
     * @param teacherId ID del profesor.
     * @param currentUser Usuario autenticado.
     * @return Lista de posts.
     */
    @GetMapping("/{teacherId}/posts")
    public ResponseEntity<?> posts(@PathVariable UUID teacherId, @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.getPostsForViewer(currentUser, teacherId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Obtiene los posts de todos los profesores a los que sigue el alumno.
     * 
     * @param currentUser Usuario autenticado (alumno).
     * @return Lista de posts del tablón del alumno.
     */
    @GetMapping("/posts")
    public ResponseEntity<?> studentPosts(@AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.getPostsForStudent(currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Crea un post en el muro de clases de un profesor.
     * 
     * @param teacherId ID del profesor.
     * @param currentUser Usuario autenticado.
     * @param body Datos del post (título y contenido).
     * @return El post creado.
     */
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

    /**
     * Solicita una hora extra para una clase.
     * 
     * @param enrollmentId ID de la inscripción.
     * @param currentUser Usuario autenticado.
     * @return Resultado de la solicitud.
     */
    @PostMapping("/{enrollmentId}/request-extra-hour")
    public ResponseEntity<?> requestExtraHour(@PathVariable UUID enrollmentId,
                                              @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.requestExtraHour(enrollmentId, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Solicita un reembolso para una clase.
     * 
     * @param enrollmentId ID de la inscripción.
     * @param currentUser Usuario autenticado.
     * @return Resultado de la solicitud.
     */
    @PostMapping("/{enrollmentId}/refund-request")
    public ResponseEntity<?> requestRefund(@PathVariable UUID enrollmentId,
                                           @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(classService.requestRefund(enrollmentId, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Actualiza el estado de una inscripción.
     * 
     * @param enrollmentId ID de la inscripción.
     * @param currentUser Usuario autenticado.
     * @param body Mapa con el nuevo estado.
     * @return La inscripción actualizada.
     */
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

    /**
     * Crea una sesión programada para una clase.
     * 
     * @param enrollmentId ID de la inscripción.
     * @param currentUser Usuario autenticado.
     * @param body Datos de la sesión.
     * @return La sesión creada.
     */
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

    /**
     * Actualiza una sesión programada.
     * 
     * @param sessionId ID de la sesión.
     * @param currentUser Usuario autenticado.
     * @param body Datos a actualizar.
     * @return La sesión actualizada.
     */
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
