package com.wavii.controller;

import com.wavii.dto.bulletin.BulletinBoardResponse;
import com.wavii.dto.bulletin.BulletinTeacherResponse;
import com.wavii.dto.bulletin.BulletinUpdateRequest;
import com.wavii.model.User;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bulletin")
@RequiredArgsConstructor
@Slf4j
public class BulletinController {

    private static final int FREE_BULLETIN_LIMIT = 3;
    private static final String UPLOAD_DIR = "/app/uploads/bulletin/";

    private final UserRepository userRepository;

    @Value("${wavii.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    /**
     * Obtiene el listado de profesores para el tablón de anuncios.
     * 
     * @param currentUser Usuario autenticado.
     * @param query Búsqueda por texto (nombre, bio, etc.).
     * @param instrument Instrumento que imparte.
     * @param role Rol (Certificado o Particular).
     * @param modality Modalidad (Online, Presencial, Ambas).
     * @param availability Disponibilidad horaria.
     * @param city Ciudad para clases presenciales.
     * @return Listado filtrado y paginado según el plan del usuario.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<BulletinBoardResponse> getTeachers(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "instrument", required = false) String instrument,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "modality", required = false) String modality,
            @RequestParam(value = "availability", required = false) String availability,
            @RequestParam(value = "city", required = false) String city) {
// ... (omitted code for brevity, but I will replace the entire block)
        List<BulletinTeacherResponse> allTeachers = userRepository
                .findByRoleIn(List.of(Role.PROFESOR_PARTICULAR, Role.PROFESOR_CERTIFICADO))
                .stream()
                .map(this::toResponse)
                .filter(teacher -> matchesQuery(teacher, query))
                .filter(teacher -> matchesInstrument(teacher, instrument))
                .filter(teacher -> matchesRole(teacher, role))
                .filter(teacher -> matchesModality(teacher, modality))
                .filter(teacher -> matchesAvailability(teacher, availability))
                .filter(teacher -> matchesCity(teacher, city))
                .sorted(Comparator.comparing((BulletinTeacherResponse t) ->
                        "profesor_certificado".equalsIgnoreCase(t.role()) ? 0 : 1)
                        .thenComparing(BulletinTeacherResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        boolean hasFullAccess = hasScholarAccess(currentUser);
        boolean canPublish = canPublish(currentUser);

        List<BulletinTeacherResponse> visibleTeachers = hasFullAccess
                ? allTeachers
                : allTeachers.stream().limit(FREE_BULLETIN_LIMIT).toList();

        return ResponseEntity.ok(new BulletinBoardResponse(
                visibleTeachers,
                hasFullAccess,
                canPublish,
                FREE_BULLETIN_LIMIT,
                allTeachers.size(),
                Math.max(0, allTeachers.size() - visibleTeachers.size()),
                "scholar"));
    }

    /**
     * Obtiene el perfil público de un profesor.
     * 
     * @param teacherId ID del profesor.
     * @return El perfil del profesor.
     */
    @GetMapping("/{teacherId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTeacher(@PathVariable UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .filter(user -> user.getRole() == Role.PROFESOR_PARTICULAR
                        || user.getRole() == Role.PROFESOR_CERTIFICADO)
                .orElse(null);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profesor no encontrado"));
        }
        return ResponseEntity.ok(toResponse(teacher));
    }

    /**
     * Actualiza el perfil de profesor en el tablón.
     * 
     * @param currentUser Usuario autenticado.
     * @param request Datos del perfil a actualizar.
     * @return El perfil actualizado.
     */
    @PostMapping
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody BulletinUpdateRequest request) {
        if (!canPublish(currentUser)) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "error",
                            "Solo los usuarios con suscripción Scholar pueden publicar en el tablón",
                            "requiredPlan", "scholar"));
        }

        String modality = normalize(request.classModality());
        String city = normalize(request.city());
        if (("PRESENCIAL".equals(modality) || "AMBAS".equals(modality)) && (city == null || city.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "La ciudad es obligatoria si la modalidad es presencial o ambas"));
        }
        if (isBlank(request.province())) {
            return ResponseEntity.badRequest().body(Map.of("message", "La provincia es obligatoria"));
        }
        if (isBlank(request.contactEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo de contacto es obligatorio"));
        }
        if (isBlank(request.contactPhone())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El número de contacto es obligatorio"));
        }
        if (!isValidEmail(request.contactEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo de contacto no es válido"));
        }
        if (!isValidPhone(request.contactPhone())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El número de contacto no es válido"));
        }

        if (currentUser.getRole() == Role.USUARIO) {
            currentUser.setRole(Role.PROFESOR_PARTICULAR);
        }

        currentUser.setBio(normalize(request.bio()));
        currentUser.setInstrument(normalize(request.instrument()));
        currentUser.setPricePerHour(request.pricePerHour());
        currentUser.setCity(city);
        currentUser.setLatitude(request.latitude());
        currentUser.setLongitude(request.longitude());
        currentUser.setAddress(normalize(request.address()));
        currentUser.setProvince(normalize(request.province()));
        currentUser.setContactEmail(normalize(request.contactEmail()));
        currentUser.setContactPhone(normalize(request.contactPhone()));
        currentUser.setInstagramUrl(normalize(request.instagramUrl()));
        currentUser.setTiktokUrl(normalize(request.tiktokUrl()));
        currentUser.setYoutubeUrl(normalize(request.youtubeUrl()));
        currentUser.setFacebookUrl(normalize(request.facebookUrl()));
        currentUser.setBannerImageUrl(normalize(request.bannerImageUrl()));
        currentUser.setPlaceImageUrls(safeImageUrls(request.placeImageUrls()));
        currentUser.setAvailabilityPreference(normalizeAvailability(request.availabilityPreference()));
        currentUser.setAvailabilityNotes(normalize(request.availabilityNotes()));
        currentUser.setClassModality(modality);

        User saved = userRepository.save(currentUser);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Sube una imagen para el perfil del profesor (banner o fotos del lugar).
     * 
     * @param file Archivo de imagen.
     * @param kind Tipo de imagen ("banner" o "place").
     * @return URL de la imagen subida.
     */
    @PostMapping("/images")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "kind", required = false) String kind) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo no puede estar vacío"));
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String original = file.getOriginalFilename();
            String extension = "";
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.'));
            }

            String prefix = "banner".equalsIgnoreCase(kind) ? "banner" : "place";
            String storedName = prefix + "_" + UUID.randomUUID() + extension;
            Path destination = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String url = appBaseUrl + "/uploads/bulletin/" + storedName;
            return ResponseEntity.ok(Map.of("url", url, "fileName", storedName));
        } catch (IOException e) {
            log.error("Error subiendo imagen del tablón: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "No se pudo guardar la imagen"));
        }
    }

    /** Convierte un usuario a su respuesta DTO para el tablón. */
    private BulletinTeacherResponse toResponse(User user) {
        return new BulletinTeacherResponse(
                user.getId().toString(),
                user.getName(),
                user.getRole().name().toLowerCase(),
                user.getBio(),
                user.getInstrument(),
                user.getPricePerHour(),
                user.getCity(),
                user.getLatitude(),
                user.getLongitude(),
                user.getAddress(),
                user.getProvince(),
                user.getContactEmail(),
                user.getContactPhone(),
                user.getInstagramUrl(),
                user.getTiktokUrl(),
                user.getYoutubeUrl(),
                user.getFacebookUrl(),
                user.getBannerImageUrl(),
                safeImageUrls(user.getPlaceImageUrls()),
                user.getAvailabilityPreference(),
                user.getAvailabilityNotes(),
                user.getClassModality());
    }

    /** Comprueba si el usuario tiene acceso completo al tablón (Plan Scholar). */
    private boolean hasScholarAccess(User user) {
        return user != null && user.getSubscription() == Subscription.EDUCATION;
    }

    /** Comprueba si el usuario puede publicar en el tablón. */
    private boolean canPublish(User user) {
        return user != null && hasScholarAccess(user);
    }

    /** Filtra por texto en varios campos del profesor. */
    private boolean matchesQuery(BulletinTeacherResponse teacher, String query) {
        if (isBlank(query)) return true;
        String q = query.trim().toLowerCase();
        return contains(teacher.name(), q)
                || contains(teacher.bio(), q)
                || contains(teacher.instrument(), q)
                || contains(teacher.city(), q)
                || contains(teacher.province(), q);
    }

    /** Filtra por instrumento. */
    private boolean matchesInstrument(BulletinTeacherResponse teacher, String instrument) {
        if (isBlank(instrument) || "Todos".equalsIgnoreCase(instrument)) return true;
        return contains(teacher.instrument(), instrument.trim().toLowerCase());
    }

    /** Filtra por rol (Certificado/Particular). */
    private boolean matchesRole(BulletinTeacherResponse teacher, String role) {
        if (isBlank(role) || "Todos".equalsIgnoreCase(role)) return true;
        if ("Certificados".equalsIgnoreCase(role)) return "profesor_certificado".equalsIgnoreCase(teacher.role());
        if ("Particulares".equalsIgnoreCase(role)) return "profesor_particular".equalsIgnoreCase(teacher.role());
        return teacher.role().equalsIgnoreCase(role.trim());
    }

    /** Filtra por modalidad de clase. */
    private boolean matchesModality(BulletinTeacherResponse teacher, String modality) {
        if (isBlank(modality) || "Todos".equalsIgnoreCase(modality)) return true;
        return teacher.classModality() != null && teacher.classModality().equalsIgnoreCase(modality.trim());
    }

    /** Filtra por disponibilidad horaria. */
    private boolean matchesAvailability(BulletinTeacherResponse teacher, String availability) {
        if (isBlank(availability) || "Todos".equalsIgnoreCase(availability)) return true;
        return teacher.availabilityPreference() != null
                && teacher.availabilityPreference().equalsIgnoreCase(availability.trim());
    }

    /** Filtra por ciudad. */
    private boolean matchesCity(BulletinTeacherResponse teacher, String city) {
        if (isBlank(city)) return true;
        return contains(teacher.city(), city.trim().toLowerCase());
    }

    /** Comprueba si una cadena contiene otra (case-insensitive). */
    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    /** Comprueba si una cadena es nula o vacía. */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Normaliza una cadena quitando espacios y devolviendo null si queda vacía. */
    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Valida el formato de un email. */
    private boolean isValidEmail(String value) {
        return value != null && value.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    /** Valida el formato de un número de teléfono. */
    private boolean isValidPhone(String value) {
        return value != null && value.trim().matches("^\\+?[0-9\\s-]{7,20}$");
    }

    /** Filtra y limpia una lista de URLs de imágenes. */
    private List<String> safeImageUrls(List<String> urls) {
        if (urls == null) return new ArrayList<>();
        return urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .toList();
    }

    /** Normaliza el valor de preferencia de disponibilidad. */
    private String normalizeAvailability(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase()) {
            case "MORNING", "AFTERNOON", "ANYTIME", "CUSTOM" -> normalized.toUpperCase();
            default -> null;
        };
    }
}
