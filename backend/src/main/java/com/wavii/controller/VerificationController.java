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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private static final String UPLOAD_DIR = "/app/uploads/verifications/";
    private static final String PDF_MIME_TYPE = "application/pdf";

    private final VerificationRequestRepository verificationRequestRepository;
    private final UserRepository userRepository;
    private final OdooService odooService;
    private final EmailService emailService;

    @Value("${wavii.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${odoo.webhook-secret:}")
    private String odooWebhookSecret;

    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("document") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo no puede estar vacio"));
        }

        String originalFileName = file.getOriginalFilename();
        if (!isPdfFile(file.getContentType(), originalFileName)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Solo se aceptan archivos PDF"));
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String extension = getFileExtension(originalFileName);
            String storedName = currentUser.getId() + "_" + UUID.randomUUID() + extension;
            Path destination = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            byte[] fileBytes = Files.readAllBytes(destination);

            String documentUrl = appBaseUrl + "/uploads/verifications/" + storedName;
            String normalizedFileName = originalFileName != null ? originalFileName : storedName;

            VerificationRequest request = VerificationRequest.builder()
                    .user(currentUser)
                    .fileName(normalizedFileName)
                    .filePath(destination.toString())
                    .status(VerificationStatus.PENDING)
                    .build();
            verificationRequestRepository.save(request);

            odooService.createVerificationTask(
                    currentUser.getId().toString(),
                    currentUser.getName(),
                    currentUser.getEmail(),
                    normalizedFileName,
                    PDF_MIME_TYPE,
                    fileBytes,
                    documentUrl
            );

            log.info("Documento PDF subido para verificacion: usuario={} archivo={}", currentUser.getEmail(), storedName);

            return ResponseEntity.ok(Map.of(
                    "message", "Documento enviado para revision",
                    "fileName", normalizedFileName,
                    "documentUrl", documentUrl,
                    "status", VerificationStatus.PENDING.name()
            ));
        } catch (IOException e) {
            log.error("Error guardando documento de verificacion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al guardar el documento"));
        }
    }

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
        log.info("Verificacion aprobada para usuario={}", user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verificacion aprobada"));
    }

    @PostMapping("/odoo-webhook")
    public ResponseEntity<?> odooWebhook(
            @RequestHeader(value = "X-Odoo-Secret", required = false) String receivedSecret,
            @RequestBody Map<String, String> body) {

        if (odooWebhookSecret == null || odooWebhookSecret.isBlank()
                || !odooWebhookSecret.equals(receivedSecret)) {
            log.warn("Odoo webhook: secreto invalido o ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Secreto no valido"));
        }

        String userIdStr = body.get("userId");
        String action = body.get("action");

        if (userIdStr == null || action == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Se requieren 'userId' y 'action'"));
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId invalido"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado"));
        }

        switch (action.toLowerCase(Locale.ROOT)) {
            case "approve" -> {
                verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(user).ifPresent(req -> {
                    req.setStatus(VerificationStatus.APPROVED);
                    verificationRequestRepository.save(req);
                });
                user.setTeacherVerified(true);
                user.setRole(Role.PROFESOR_CERTIFICADO);
                userRepository.save(user);
                emailService.sendVerificationApprovedEmail(user.getEmail(), user.getName());
                log.info("Odoo webhook: verificacion APROBADA para usuario={}", user.getEmail());
                return ResponseEntity.ok(Map.of("message", "Verificacion aprobada"));
            }
            case "reject" -> {
                verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(user).ifPresent(req -> {
                    req.setStatus(VerificationStatus.REJECTED);
                    verificationRequestRepository.save(req);
                });
                userRepository.save(user);
                emailService.sendVerificationRejectedEmail(user.getEmail(), user.getName());
                log.info("Odoo webhook: verificacion RECHAZADA para usuario={}", user.getEmail());
                return ResponseEntity.ok(Map.of("message", "Verificacion rechazada"));
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Accion no valida. Usa 'approve' o 'reject'"));
            }
        }
    }

    private boolean isPdfFile(String contentType, String fileName) {
        if (contentType == null || !PDF_MIME_TYPE.equalsIgnoreCase(contentType.trim())) {
            return false;
        }
        String extension = getFileExtension(fileName);
        return ".pdf".equals(extension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        String safeName = Path.of(fileName).getFileName().toString();
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return safeName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
