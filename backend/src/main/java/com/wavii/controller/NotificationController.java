package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sesion no valida"));
        }
        return ResponseEntity.ok(Map.of(
                "items", notificationService.list(currentUser),
                "summary", notificationService.summary(currentUser)
        ));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markRead(@AuthenticationPrincipal User currentUser,
                                      @PathVariable UUID notificationId) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sesion no valida"));
        }
        try {
            return ResponseEntity.ok(notificationService.markRead(currentUser, notificationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sesion no valida"));
        }
        return ResponseEntity.ok(notificationService.markAllRead(currentUser));
    }

    @DeleteMapping
    public ResponseEntity<?> clearAll(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sesion no valida"));
        }
        return ResponseEntity.ok(notificationService.clearAll(currentUser));
    }
}
