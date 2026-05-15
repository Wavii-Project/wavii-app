package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherReportController {

    private final ClassService classService;

    @PostMapping("/{teacherId}/reports")
    public ResponseEntity<?> reportTeacher(@PathVariable UUID teacherId,
                                           @AuthenticationPrincipal User currentUser,
                                           @RequestBody Map<String, String> body) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sesion no valida"));
        }
        try {
            return ResponseEntity.ok(classService.createTeacherReport(teacherId, currentUser, body));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}
