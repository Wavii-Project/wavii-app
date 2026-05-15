package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.model.VerificationRequest;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.VerificationStatus;
import com.wavii.repository.UserRepository;
import com.wavii.repository.VerificationRequestRepository;
import com.wavii.service.EmailService;
import com.wavii.service.OdooService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private static final String UPLOAD_DIR = "/app/uploads/verifications/";

    private final VerificationRequestRepository verificationRequestRepository;
    private final UserRepository userRepository;
    private final OdooService odooService;
    private final EmailService emailService;

    @Value("${wavii.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${odoo.webhook-secret:}")
    private String odooWebhookSecret;

    // ─────────────────────────────────────────────────────────────
    // Subida de documento (requiere JWT)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("document") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo no puede estar vacío"));
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String extension = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.'));
            }
            String storedName = currentUser.getId() + "_" + UUID.randomUUID() + extension;
            Path destination = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String documentUrl = appBaseUrl + "/uploads/verifications/" + storedName;

            VerificationRequest request = VerificationRequest.builder()
                    .user(currentUser)
                    .fileName(original != null ? original : storedName)
                    .filePath(destination.toString())
                    .status(VerificationStatus.PENDING)
                    .build();
            verificationRequestRepository.save(request);

            odooService.createVerificationTask(
                    currentUser.getName(), currentUser.getEmail(),
                    original != null ? original : storedName, documentUrl);

            log.info("Documento subido para verificación: usuario={} archivo={}", currentUser.getEmail(), storedName);

            return ResponseEntity.ok(Map.of(
                    "message", "Documento enviado para revisión",
                    "fileName", original != null ? original : storedName,
                    "documentUrl", documentUrl,
                    "status", VerificationStatus.PENDING.name()
            ));

        } catch (IOException e) {
            log.error("Error guardando documento de verificación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al guardar el documento"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Estado de la verificación (requiere JWT)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal User currentUser) {
        return verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser)
                .map(req -> ResponseEntity.ok(Map.of(
                        "status", req.getStatus().name(),
                        "fileName", req.getFileName(),
                        "createdAt", req.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.ok(Map.of("status", "NONE")));
    }

    // ─────────────────────────────────────────────────────────────
    // Aprobación manual desde panel admin (requiere JWT + ADMIN)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveVerification(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(user).ifPresent(req -> {
            req.setStatus(VerificationStatus.APPROVED);
            verificationRequestRepository.save(req);
        });

        user.setTeacherVerified(true);
        user.setRole(Role.PROFESOR_CERTIFICADO);
        userRepository.save(user);

        emailService.sendVerificationApprovedEmail(user.getEmail(), user.getName());
        log.info("Verificación aprobada para usuario={}", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verificación aprobada"));
    }

    // ─────────────────────────────────────────────────────────────
    // Webhook de Odoo — aprobar o rechazar desde el ERP
    // Protegido por secreto compartido, sin JWT
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/odoo-webhook")
    public ResponseEntity<?> odooWebhook(
            @RequestHeader(value = "X-Odoo-Secret", required = false) String receivedSecret,
            @RequestBody Map<String, String> body) {

        if (odooWebhookSecret == null || odooWebhookSecret.isBlank()
                || !odooWebhookSecret.equals(receivedSecret)) {
            log.warn("Odoo webhook: secreto inválido o ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Secreto no válido"));
        }

        String userIdStr = body.get("userId");
        String action    = body.get("action");

        if (userIdStr == null || action == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Se requieren 'userId' y 'action'"));
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId inválido"));
        }

        User user = userRepository.findById(userId)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado"));
        }

        switch (action.toLowerCase()) {
            case "approve" -> {
                verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(user).ifPresent(req -> {
                    req.setStatus(VerificationStatus.APPROVED);
                    verificationRequestRepository.save(req);
                });
                user.setTeacherVerified(true);
                user.setRole(Role.PROFESOR_CERTIFICADO);
                userRepository.save(user);
                emailService.sendVerificationApprovedEmail(user.getEmail(), user.getName());
                log.info("Odoo webhook: verificación APROBADA para usuario={}", user.getEmail());
                return ResponseEntity.ok(Map.of("message", "Verificación aprobada"));
            }
            case "reject" -> {
                verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(user).ifPresent(req -> {
                    req.setStatus(VerificationStatus.REJECTED);
                    verificationRequestRepository.save(req);
                });
                // No cambia el rol — el usuario mantiene acceso a Scholar como profesor
                userRepository.save(user);
                emailService.sendVerificationRejectedEmail(user.getEmail(), user.getName());
                log.info("Odoo webhook: verificación RECHAZADA para usuario={}", user.getEmail());
                return ResponseEntity.ok(Map.of("message", "Verificación rechazada"));
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Acción no válida. Usa 'approve' o 'reject'"));
            }
        }
    }
}
