package com.wavii.controller;

import com.wavii.dto.band.BandListingResponse;
import com.wavii.dto.band.CreateBandListingRequest;
import com.wavii.model.User;
import com.wavii.service.BandListingService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/bands")
@RequiredArgsConstructor
public class BandListingController {

    private final BandListingService service;

    @Value("${wavii.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${pdf.storage.path:./uploads/pdfs}")
    private String pdfStoragePath;

    @GetMapping
    public ResponseEntity<Page<BandListingResponse>> getListings(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(service.getListings(genre, city, role, page));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BandListingResponse>> getMyListings(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(service.getMyListings(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BandListingResponse> getListing(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<BandListingResponse> create(
            @RequestBody CreateBandListingRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(201).body(service.create(request, currentUser));
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        service.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return extension.isBlank() ? ".jpg" : extension;
    }

    private Path uploadRoot() {
        Path root = Paths.get(pdfStoragePath).getParent();
        return root != null ? root : Paths.get("uploads");
    }
}
