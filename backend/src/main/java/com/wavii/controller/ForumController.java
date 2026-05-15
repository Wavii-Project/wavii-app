package com.wavii.controller;

import com.wavii.dto.forum.CreateForumRequest;
import com.wavii.dto.forum.CreatePostRequest;
import com.wavii.dto.forum.ForumResponse;
import com.wavii.dto.forum.ForumSummaryResponse;
import com.wavii.dto.forum.PostResponse;
import com.wavii.dto.forum.UpdateForumMemberRoleRequest;
import com.wavii.model.User;
import com.wavii.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
@RequestMapping("/api/forums")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @Value("${wavii.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${pdf.storage.path:./uploads/pdfs}")
    private String pdfStoragePath;

    @GetMapping
    public ResponseEntity<List<ForumSummaryResponse>> getForums(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.getForums(search, city, category, sort, currentUser));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ForumSummaryResponse>> getMyForums(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.getMyForums(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumResponse> getForum(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.getForumById(id, currentUser));
    }

    @PostMapping
    public ResponseEntity<ForumResponse> createForum(
            @RequestBody CreateForumRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(201).body(forumService.createForum(request, currentUser));
    }

    @PostMapping("/images")
    public ResponseEntity<?> uploadForumImage(@RequestParam("file") MultipartFile file) {
        return uploadImage(file, "forums");
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinForum(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        forumService.joinForum(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/join")
    public ResponseEntity<Void> leaveForum(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        forumService.leaveForum(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ForumResponse> likeForum(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.likeForum(id, currentUser));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ForumResponse> unlikeForum(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.unlikeForum(id, currentUser));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.getMembers(id, currentUser));
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<ForumResponse> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestBody UpdateForumMemberRoleRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.updateMemberRole(id, userId, request.role(), currentUser));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User currentUser
    ) {
        forumService.removeMember(id, userId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<Page<PostResponse>> getPosts(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(forumService.getPosts(id, page, currentUser));
    }

    @PostMapping("/{id}/posts")
    public ResponseEntity<PostResponse> createPost(
            @PathVariable UUID id,
            @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(201).body(forumService.createPost(id, request, currentUser));
    }

    private ResponseEntity<?> uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo no puede estar vacio"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Solo se permiten imagenes"));
        }

        try {
            Path uploadPath = uploadRoot().resolve(folder);
            Files.createDirectories(uploadPath);
            String extension = extensionOf(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), uploadPath.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(Map.of(
                    "url", appBaseUrl + "/uploads/" + folder + "/" + storedName,
                    "fileName", storedName
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "No se pudo guardar la imagen"));
        }
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
