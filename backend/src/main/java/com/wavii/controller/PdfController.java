package com.wavii.controller;

import com.wavii.dto.pdf.PdfResponseDto;
import com.wavii.model.User;
import com.wavii.service.PdfStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pdfs")
@RequiredArgsConstructor
public class PdfController {

    private final PdfStorageService pdfStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PdfResponseDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "songTitle", required = false) String songTitle,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "difficulty", defaultValue = "1") int difficulty,
            @AuthenticationPrincipal User currentUser) {
        try {
            PdfResponseDto dto = pdfStorageService.save(file, coverImage, currentUser, songTitle, description, difficulty);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/public")
    public ResponseEntity<List<PdfResponseDto>> publicFeed(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false, defaultValue = "NEWEST") String sort,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(pdfStorageService.getPublicFeed(search, difficulty, sort, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PdfResponseDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(pdfStorageService.getByIdForUser(id, currentUser));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PdfResponseDto>> list(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(pdfStorageService.listByUser(currentUser));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        try {
            Resource resource = pdfStorageService.loadAsResource(id);
            String filename = pdfStorageService.findById(id).getOriginalName();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline().filename(filename).build().toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<PdfResponseDto> like(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(pdfStorageService.like(id, currentUser));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<PdfResponseDto> unlike(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(pdfStorageService.unlike(id, currentUser));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            pdfStorageService.delete(id, currentUser);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
