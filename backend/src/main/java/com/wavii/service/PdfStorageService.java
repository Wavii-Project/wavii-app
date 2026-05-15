package com.wavii.service;

import com.wavii.dto.pdf.PdfResponseDto;
import com.wavii.model.PdfDocument;
import com.wavii.model.PdfLike;
import com.wavii.model.User;
import com.wavii.repository.PdfDocumentRepository;
import com.wavii.repository.PdfLikeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para el almacenamiento y gestión de archivos PDF (tablaturas).
 * Maneja la subida de archivos, generación de portadas y metadatos asociados.
 * 
 * @author danielrguezh
 */
@Service
@RequiredArgsConstructor
public class PdfStorageService {

    private final PdfDocumentRepository repository;
    private final PdfLikeRepository likeRepository;
    private final com.wavii.repository.PdfReportRepository reportRepository;

    @Value("${pdf.storage.path:./uploads/pdfs}")
    private String storagePath;

    /**
     * Guarda un nuevo archivo PDF (tablatura) en el sistema, incluyendo metadatos y portada opcional.
     * 
     * @param file Archivo PDF subido.
     * @param coverImage Imagen de portada opcional.
     * @param owner Usuario propietario del documento.
     * @param songTitle Título de la canción.
     * @param description Descripción opcional.
     * @param difficulty Nivel de dificultad (1-3).
     * @return Respuesta con los datos del PDF guardado.
     * @throws IOException Si ocurre un error al escribir el archivo en disco.
     */
    @Transactional
    public PdfResponseDto save(
            MultipartFile file,
            MultipartFile coverImage,
            User owner,
            String songTitle,
            String description,
            int difficulty
    ) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("El archivo no puede estar vacio");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }
        if (difficulty < 1 || difficulty > 3) difficulty = 1;

        Path storageDir = Paths.get(storagePath);
        Files.createDirectories(storageDir);

        String fileName = UUID.randomUUID() + ".pdf";
        Path filePath = storageDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String coverImagePath = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverType = coverImage.getContentType();
            if (coverType == null || !coverType.startsWith("image/")) {
                throw new IllegalArgumentException("La portada debe ser una imagen");
            }

            Path coverDir = storageDir.resolve("covers");
            Files.createDirectories(coverDir);
            String coverExt = getExtension(coverImage.getOriginalFilename(), ".jpg");
            String coverName = UUID.randomUUID() + coverExt;
            Path coverPath = coverDir.resolve(coverName);
            Files.copy(coverImage.getInputStream(), coverPath, StandardCopyOption.REPLACE_EXISTING);
            coverImagePath = "pdfs/covers/" + coverName;
        }

        int pageCount = 0;
        try (PDDocument pdf = Loader.loadPDF(filePath.toFile())) {
            pageCount = pdf.getNumberOfPages();
        } catch (Exception ignored) {
        }

        PdfDocument doc = PdfDocument.builder()
                .originalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : fileName)
                .fileName(fileName)
                .filePath(filePath.toAbsolutePath().toString())
                .fileSize(file.getSize())
                .pageCount(pageCount)
                .uploadedAt(LocalDateTime.now())
                .songTitle(songTitle != null ? songTitle.strip() : null)
                .description(description != null && !description.isBlank() ? description.strip() : null)
                .coverImagePath(coverImagePath)
                .difficulty(difficulty)
                .likeCount(0)
                .owner(owner)
                .build();

        return PdfResponseDto.from(repository.save(doc));
    }

    /**
     * Obtiene el feed público de tablaturas con filtros y ordenación opcional.
     * 
     * @param search Término de búsqueda (título).
     * @param difficulty Nivel de dificultad para filtrar.
     * @param sort Criterio de ordenación (NEWEST, MOST_LIKED, etc.).
     * @param currentUser Usuario actual para marcar sus "likes".
     * @return Lista de tablaturas que coinciden con los criterios.
     */
    @Transactional(readOnly = true)
    public List<PdfResponseDto> getPublicFeed(String search, Integer difficulty, String sort, User currentUser) {
        String q = (search != null && !search.isBlank()) ? search.strip() : null;
        Sort sortOrder = switch (sort != null ? sort : "NEWEST") {
            case "OLDEST" -> Sort.by(Sort.Direction.ASC, "uploadedAt");
            case "MOST_LIKED" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "LEAST_LIKED" -> Sort.by(Sort.Direction.ASC, "likeCount");
            default -> Sort.by(Sort.Direction.DESC, "uploadedAt");
        };
        Pageable page = PageRequest.of(0, 50, sortOrder);
        List<PdfDocument> docs;
        if (q != null && difficulty != null) {
            docs = repository.findPublicFeedBySearchAndDifficulty(q, difficulty, page);
        } else if (q != null) {
            docs = repository.findPublicFeedBySearch(q, page);
        } else if (difficulty != null) {
            docs = repository.findPublicFeedByDifficulty(difficulty, page);
        } else {
            docs = repository.findAllPublicFeed(page);
        }

        Set<Long> likedIds = new HashSet<>();
        if (currentUser != null && !docs.isEmpty()) {
            List<Long> ids = docs.stream().map(PdfDocument::getId).collect(Collectors.toList());
            likedIds = new HashSet<>(likeRepository.findLikedPdfIds(currentUser.getId(), ids));
        }

        final Set<Long> liked = likedIds;
        return docs.stream()
                .map(d -> PdfResponseDto.from(d, liked.contains(d.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una tablatura por ID marcando si gusta al usuario actual.
     * 
     * @param id ID del documento.
     * @param currentUser Usuario actual.
     * @return Datos del PDF.
     */
    @Transactional(readOnly = true)
    public PdfResponseDto getByIdForUser(Long id, User currentUser) {
        PdfDocument doc = findById(id);
        boolean likedByMe =
                currentUser != null &&
                        likeRepository.existsByPdfIdAndUserId(id, currentUser.getId());
        return PdfResponseDto.from(doc, likedByMe);
    }

    /**
     * Lista todas las tablaturas (privadas y públicas) de un usuario.
     * 
     * @param owner Usuario propietario.
     * @return Lista de tablaturas del usuario.
     */
    @Transactional(readOnly = true)
    public List<PdfResponseDto> listByUser(User owner) {
        return repository.findByOwnerOrderByUploadedAtDesc(owner)
                .stream()
                .map(PdfResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene las tablaturas públicas de un usuario específico para vista de perfil.
     * 
     * @param userId ID del usuario dueño.
     * @param viewer Usuario que consulta.
     * @return Lista de tablaturas públicas.
     */
    @Transactional(readOnly = true)
    public List<PdfResponseDto> getPublicTabsByUser(UUID userId, User viewer) {
        List<PdfDocument> docs = repository.findPublicByOwnerIdOrderByUploadedAtDesc(userId);
        if (docs.isEmpty()) return List.of();

        Set<Long> likedIds = new HashSet<>();
        if (viewer != null) {
            List<Long> ids = docs.stream().map(PdfDocument::getId).collect(Collectors.toList());
            likedIds.addAll(likeRepository.findLikedPdfIds(viewer.getId(), ids));
        }
        return docs.stream()
                .map(doc -> PdfResponseDto.from(doc, likedIds.contains(doc.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Registra un "me gusta" en una tablatura.
     * 
     * @param id ID de la tablatura.
     * @param user Usuario que da like.
     * @return Datos actualizados del PDF.
     */
    @Transactional
    public PdfResponseDto like(Long id, User user) {
        PdfDocument doc = findById(id);
        if (!likeRepository.existsByPdfIdAndUserId(id, user.getId())) {
            likeRepository.save(PdfLike.builder().pdf(doc).user(user).likedAt(LocalDateTime.now()).build());
            repository.incrementLikeCount(id);
            doc.setLikeCount(doc.getLikeCount() + 1);
        }
        return PdfResponseDto.from(doc, true);
    }

    /**
     * Retira un "me gusta" de una tablatura.
     * 
     * @param id ID de la tablatura.
     * @param user Usuario que retira el like.
     * @return Datos actualizados del PDF.
     */
    @Transactional
    public PdfResponseDto unlike(Long id, User user) {
        PdfDocument doc = findById(id);
        likeRepository.findByPdfIdAndUserId(id, user.getId()).ifPresent(like -> {
            likeRepository.delete(like);
            repository.decrementLikeCount(id);
            doc.setLikeCount(Math.max(0, doc.getLikeCount() - 1));
        });
        return PdfResponseDto.from(doc, false);
    }

    /**
     * Busca un documento PDF por su ID interno.
     */
    public PdfDocument findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PDF no encontrado con id: " + id));
    }

    /**
     * Carga el archivo PDF desde el disco como recurso para su descarga.
     * 
     * @param id ID del documento.
     * @return Recurso descargable.
     * @throws MalformedURLException Si la ruta del archivo es inválida.
     */
    public Resource loadAsResource(Long id) throws MalformedURLException {
        PdfDocument doc = findById(id);
        Path path = Paths.get(doc.getFilePath());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) throw new RuntimeException("Archivo no encontrado en disco: " + doc.getFileName());
        return resource;
    }

    /**
     * Elimina un archivo PDF del sistema (disco y base de datos).
     * 
     * @param id ID del documento.
     * @param owner Usuario que solicita el borrado (debe ser el dueño).
     * @throws IOException Si falla la eliminación física del archivo.
     */
    @Transactional
    public void delete(Long id, User owner) throws IOException {
        PdfDocument doc = repository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new SecurityException("No autorizado o PDF no encontrado"));
        Files.deleteIfExists(Paths.get(doc.getFilePath()));
        if (doc.getCoverImagePath() != null && !doc.getCoverImagePath().isBlank()) {
            Files.deleteIfExists(Paths.get("/app/uploads").resolve(doc.getCoverImagePath().substring("pdfs/".length())));
        }
        likeRepository.deleteByPdfId(id);
        reportRepository.deleteByPdfDocumentId(id);
        repository.delete(doc);
    }

    private String getExtension(String originalFilename, String fallback) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return fallback;
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        return ext.isBlank() ? fallback : ext;
    }
}
