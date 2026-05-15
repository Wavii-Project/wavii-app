package com.wavii.controller;

import com.wavii.dto.band.BandListingResponse;
import com.wavii.dto.band.CreateBandListingRequest;
import com.wavii.model.User;
import com.wavii.service.BandListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para la gestión de anuncios de bandas y músicos.
 * 
 * @author danielrguezh
 */
@RestController
@RequestMapping("/api/band-listings")
@RequiredArgsConstructor
@Slf4j
public class BandListingController {

    private final BandListingService service;

    @Value("${wavii.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${pdf.storage.path:./uploads/pdfs}")
    private String pdfStoragePath;

    /**
     * Obtiene un listado paginado de anuncios de bandas.
     * 
     * @param genre Género musical (opcional).
     * @param city Ciudad (opcional).
     * @param role Rol del músico (opcional).
     * @param page Número de página.
     * @return Página de resultados.
     */
    @GetMapping
    public ResponseEntity<Page<BandListingResponse>> getListings(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(service.getListings(genre, city, role, page));
    }

    /**
     * Obtiene los anuncios creados por el usuario actual.
     * 
     * @param currentUser Usuario autenticado.
     * @return Lista de anuncios del usuario.
     */
    @GetMapping("/my")
    public ResponseEntity<List<BandListingResponse>> getMyListings(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.getMyListings(currentUser));
    }

    /**
     * Obtiene un anuncio por su ID.
     * 
     * @param id ID del anuncio.
     * @return El anuncio encontrado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BandListingResponse> getListing(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    /**
     * Crea un nuevo anuncio de banda.
     * 
     * @param request Datos del anuncio.
     * @param currentUser Usuario que crea el anuncio.
     * @return El anuncio creado.
     */
    @PostMapping
    public ResponseEntity<BandListingResponse> create(
            @RequestBody CreateBandListingRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(201).body(service.create(request, currentUser));
    }

    /**
     * Sube una imagen para un anuncio de banda.
     * 
     * @param file Archivo de imagen.
     * @return URL de la imagen subida.
     */
    @PostMapping("/images")
    public ResponseEntity<?> uploadBandImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo no puede estar vacio"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Solo se permiten imagenes"));
        }

        try {
            Path uploadPath = uploadRoot().resolve("bands");
            Files.createDirectories(uploadPath);
            String storedName = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
            Files.copy(file.getInputStream(), uploadPath.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(Map.of(
                    "url", appBaseUrl + "/uploads/bands/" + storedName,
                    "fileName", storedName
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "No se pudo guardar la imagen"));
        }
    }

    /**
     * Elimina un anuncio de banda.
     * 
     * @param id ID del anuncio a eliminar.
     * @param currentUser Usuario autenticado.
     * @return 204 No Content si se eliminó correctamente.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene la extensión de un nombre de archivo.
     * 
     * @param originalFilename Nombre original.
     * @return Extensión (por defecto .jpg).
     */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return extension.isBlank() ? ".jpg" : extension;
    }

    /**
     * Obtiene la ruta raíz para las subidas.
     * 
     * @return Path de la raíz de subidas.
     */
    private Path uploadRoot() {
        Path root = Paths.get(pdfStoragePath).getParent();
        return root != null ? root : Paths.get("uploads");
    }
}
