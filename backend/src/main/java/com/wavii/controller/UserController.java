package com.wavii.controller;

import com.wavii.dto.pdf.PdfResponseDto;
import com.wavii.dto.user.PublicUserProfileDto;
import com.wavii.model.User;
import com.wavii.model.UserBlock;
import com.wavii.model.UserReport;
import com.wavii.repository.PdfDocumentRepository;
import com.wavii.repository.UserBlockRepository;
import com.wavii.repository.UserRepository;
import com.wavii.repository.UserReportRepository;
import com.wavii.service.OdooService;
import com.wavii.service.PdfStorageService;
import com.wavii.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Controlador REST para la gestión de perfiles de usuario.
 * Proporciona endpoints para consultar, actualizar y gestionar la cuenta del usuario.
 * 
 * @author eduglezexp
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;
    private final UserBlockRepository userBlockRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfStorageService pdfStorageService;
    private final PasswordEncoder passwordEncoder;
    private final StripeService stripeService;
    private final OdooService odooService;

    /**
     * Actualiza los datos del perfil del usuario actual (nombre y ciudad).
     * 
     * @param currentUser Usuario autenticado.
     * @param body Mapa con los campos a actualizar.
     * @return 200 OK con los datos actualizados o error de validación.
     */
    @PatchMapping("/me")
    public ResponseEntity<?> updateMe(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body
    ) {
        String name = body.get("name");
        String city = body.get("city");
        if ((name == null || name.isBlank()) && (city == null || city.isBlank())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "VALIDATION_ERROR",
                            "message", "Debes indicar al menos un campo para actualizar"));
        }
        if (name != null && !name.isBlank() && name.trim().length() < 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "VALIDATION_ERROR",
                            "message", "El nombre debe tener al menos 3 caracteres"));
        }
        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            if (!currentUser.getName().equalsIgnoreCase(trimmedName)
                    && userRepository.existsByNameIgnoreCase(trimmedName)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("code", "CONFLICT",
                                "field", "name",
                                "message", "Este nombre de usuario ya está en uso"));
            }
            currentUser.setName(trimmedName);
        }
        if (city != null) {
            currentUser.setCity(city.isBlank() ? null : city.trim());
        }
        userRepository.save(currentUser);
        odooService.createSubscriptionTask(
                currentUser.getName(),
                currentUser.getEmail(),
                "Actualizacion de perfil",
                "El usuario ha actualizado su perfil visible. Nombre: " + currentUser.getName()
                        + "\nCiudad: " + (currentUser.getCity() != null ? currentUser.getCity() : "no informada")
        );
        return ResponseEntity.ok(Map.of(
                "name", currentUser.getName(),
                "city", currentUser.getCity() != null ? currentUser.getCity() : ""
        ));
    }

    /**
     * Cambia la contraseña del usuario actual.
     * 
     * @param currentUser Usuario autenticado.
     * @param req Datos con la contraseña actual y la nueva.
     * @return 200 OK si se cambió correctamente.
     */
    @PatchMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal User currentUser,
            @RequestBody ChangePasswordRequest req
    ) {
        if (currentUser.getPasswordHash() == null || currentUser.getPasswordHash().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "NO_PASSWORD",
                            "message", "Tu cuenta no tiene contraseña configurada (inicio con Google)"));
        }

        if (!passwordEncoder.matches(req.currentPassword(), currentUser.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "WRONG_PASSWORD",
                            "message", "La contraseña actual es incorrecta"));
        }

        if (req.newPassword() == null || !STRONG_PASSWORD_PATTERN.matcher(req.newPassword()).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "VALIDATION_ERROR",
                            "message", "La nueva contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"));
        }

        if (req.newPassword().equals(req.currentPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "SAME_PASSWORD",
                            "message", "La nueva contraseña debe ser diferente a la actual"));
        }

        currentUser.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(currentUser);
        log.info("Contrasena actualizada para usuario {}", currentUser.getEmail());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    /**
     * Programa la eliminación de la cuenta del usuario actual (en 15 días).
     * 
     * @param currentUser Usuario autenticado.
     * @return 200 OK con la fecha programada.
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(@AuthenticationPrincipal User currentUser) {
        try {
            if (stripeService.isConfigured()
                    && currentUser.getStripeSubscriptionId() != null
                    && !currentUser.getStripeSubscriptionId().isBlank()
                    && !"canceled".equals(currentUser.getSubscriptionStatus())) {
                try {
                    stripeService.cancelAtPeriodEnd(currentUser.getStripeSubscriptionId());
                    currentUser.setSubscriptionCancelAtPeriodEnd(true);
                    log.info("Suscripcion de {} cancelada al final del periodo por eliminacion de cuenta",
                            currentUser.getEmail());
                } catch (Exception e) {
                    log.warn("No se pudo cancelar suscripcion Stripe para {}: {}",
                            currentUser.getEmail(), e.getMessage());
                }
            }

            LocalDateTime deletionDate = LocalDateTime.now().plusDays(15);
            if (currentUser.getSubscriptionCurrentPeriodEnd() != null
                    && currentUser.getSubscriptionCurrentPeriodEnd().isAfter(deletionDate)) {
                deletionDate = currentUser.getSubscriptionCurrentPeriodEnd();
            }

            currentUser.setDeletionScheduledAt(deletionDate);
            userRepository.save(currentUser);
            odooService.createSubscriptionTask(
                    currentUser.getName(),
                    currentUser.getEmail(),
                    "Eliminacion de cuenta programada",
                    "Fecha prevista de eliminacion: " + deletionDate
                            + "\nCancelacion de suscripcion al fin de periodo: " + currentUser.isSubscriptionCancelAtPeriodEnd()
            );

            log.info("Eliminacion programada para {} el {}", currentUser.getEmail(), deletionDate);
            return ResponseEntity.ok(Map.of(
                    "deletionScheduledAt", deletionDate.toString(),
                    "message", "Tu cuenta se eliminara el " + deletionDate.toLocalDate()
            ));

        } catch (Exception e) {
            log.error("Error programando eliminacion para {}: {}", currentUser.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "No se pudo programar la eliminacion de la cuenta"));
        }
    }

    /**
     * Cancela la eliminación programada de la cuenta.
     * 
     * @param currentUser Usuario autenticado.
     * @return 200 OK si se canceló correctamente.
     */
    @PatchMapping("/me/deletion-cancel")
    public ResponseEntity<?> cancelDeletion(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getDeletionScheduledAt() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No hay ninguna eliminacion programada"));
        }

        if (stripeService.isConfigured()
                && currentUser.getStripeSubscriptionId() != null
                && !currentUser.getStripeSubscriptionId().isBlank()
                && currentUser.isSubscriptionCancelAtPeriodEnd()) {
            try {
                stripeService.reactivateSubscription(currentUser.getStripeSubscriptionId());
                currentUser.setSubscriptionCancelAtPeriodEnd(false);
                log.info("Suscripcion de {} reactivada tras cancelar eliminacion", currentUser.getEmail());
            } catch (Exception e) {
                log.warn("No se pudo reactivar suscripcion Stripe para {}: {}",
                        currentUser.getEmail(), e.getMessage());
            }
        }

        currentUser.setDeletionScheduledAt(null);
        userRepository.save(currentUser);
        odooService.createSubscriptionTask(
                currentUser.getName(),
                currentUser.getEmail(),
                "Eliminacion de cuenta cancelada",
                "El usuario ha cancelado la eliminacion programada de su cuenta."
        );
        return ResponseEntity.ok(Map.of("message", "La eliminacion de tu cuenta ha sido cancelada"));
    }

    /**
     * Comprueba si un nombre de usuario está disponible para actualización.
     * 
     * @param name Nombre a comprobar.
     * @param currentUser Usuario autenticado.
     * @return 200 OK con { taken: boolean }.
     */
    @GetMapping("/me/check-name")
    public ResponseEntity<Map<String, Boolean>> checkNameForUpdate(
            @RequestParam String name,
            @AuthenticationPrincipal User currentUser) {
        String trimmed = name.strip();
        boolean taken = userRepository.existsByNameIgnoreCase(trimmed)
                && !currentUser.getName().equalsIgnoreCase(trimmed);
        return ResponseEntity.ok(Map.of("taken", taken));
    }

    /**
     * Obtiene el perfil público de un usuario.
     * 
     * @param id ID del usuario.
     * @param currentUser Usuario autenticado.
     * @return Perfil público del usuario.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return userRepository.findById(id)
                .map(user -> {
                    int tabs = (int) pdfDocumentRepository.countByOwnerId(id);
                    return ResponseEntity.ok(PublicUserProfileDto.from(user, tabs));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene las tablaturas públicas de un usuario.
     * 
     * @param id ID del usuario.
     * @param currentUser Usuario autenticado.
     * @return Lista de tablaturas públicas.
     */
    @GetMapping("/{id}/tabs")
    public ResponseEntity<List<PdfResponseDto>> getUserTabs(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        List<PdfResponseDto> tabs = pdfStorageService.getPublicTabsByUser(id, currentUser);
        return ResponseEntity.ok(tabs);
    }

    /**
     * Reporta a un usuario por una razón específica.
     * 
     * @param id ID del usuario reportado.
     * @param body Mapa con el campo "reason".
     * @param currentUser Usuario que reporta.
     * @return 200 OK si se envió el reporte.
     */
    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportUser(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepository.findById(id).map(reported -> {
            UserReport report = UserReport.builder()
                    .reporter(currentUser)
                    .reported(reported)
                    .reason(body.get("reason"))
                    .build();
            userReportRepository.save(report);
            return ResponseEntity.ok(Map.of("message", "Reporte enviado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Bloquea a un usuario.
     * 
     * @param id ID del usuario a bloquear.
     * @param currentUser Usuario que bloquea.
     * @return 200 OK si se bloqueó correctamente.
     */
    @PostMapping("/{id}/block")
    public ResponseEntity<?> blockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepository.findById(id).map(blocked -> {
            if (!userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), id)) {
                userBlockRepository.save(UserBlock.builder()
                        .blocker(currentUser)
                        .blocked(blocked)
                        .build());
            }
            return ResponseEntity.ok(Map.of("message", "Usuario bloqueado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Desbloquea a un usuario.
     * 
     * @param id ID del usuario a desbloquear.
     * @param currentUser Usuario que desbloquea.
     * @return 200 OK si se desbloqueó correctamente.
     */
    @DeleteMapping("/{id}/block")
    public ResponseEntity<?> unblockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userBlockRepository.findByBlockerIdAndBlockedId(currentUser.getId(), id)
                .ifPresent(userBlockRepository::delete);
        return ResponseEntity.ok(Map.of("message", "Usuario desbloqueado"));
    }

    /**
     * Activa o desactiva la recepción de mensajes privados.
     * 
     * @param body Mapa con el campo "acceptsMessages".
     * @param currentUser Usuario autenticado.
     * @return 200 OK.
     */
    @PatchMapping("/me/accepts-messages")
    public ResponseEntity<?> toggleAcceptsMessages(
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Boolean accepts = body.get("acceptsMessages");
        if (accepts == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Campo acceptsMessages requerido"));
        }
        currentUser.setAcceptsMessages(accepts);
        userRepository.save(currentUser);
        return ResponseEntity.ok().build();
    }

    record ChangePasswordRequest(String currentPassword, String newPassword) {}
}
