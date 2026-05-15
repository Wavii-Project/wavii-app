package com.wavii.dto.pdf;

import com.wavii.model.PdfDocument;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO que representa la respuesta detallada de una tablatura PDF.
 * Incluye metadatos, estado de "me gusta" del usuario y datos del propietario.
 * 
 * @author danielrguezh
 */
public record PdfResponseDto(
        Long id,
        String originalName,
        String fileName,
        long fileSize,
        int pageCount,
        LocalDateTime uploadedAt,
        String songTitle,
        String description,
        String coverImageUrl,
        int difficulty,
        int likeCount,
        boolean likedByMe,
        String ownerName,
        UUID ownerId
) {
    public static PdfResponseDto from(PdfDocument doc, boolean likedByMe) {
        return new PdfResponseDto(
                doc.getId(),
                doc.getOriginalName(),
                doc.getFileName(),
                doc.getFileSize(),
                doc.getPageCount(),
                doc.getUploadedAt(),
                doc.getSongTitle(),
                doc.getDescription(),
                doc.getCoverImagePath() != null ? "/uploads/" + doc.getCoverImagePath() : null,
                doc.getDifficulty(),
                doc.getLikeCount(),
                likedByMe,
                doc.getOwner() != null ? doc.getOwner().getName() : null,
                doc.getOwner() != null ? doc.getOwner().getId() : null
        );
    }

    public static PdfResponseDto from(PdfDocument doc) {
        return from(doc, false);
    }
}
