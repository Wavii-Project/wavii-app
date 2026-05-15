package com.wavii.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa una tablatura en formato PDF subida por un usuario.
 * Contiene información sobre el archivo, metadatos musicales y estadísticas de interacción.
 * 
 * @author danielrguezh
 */
@Entity
@Table(name = "pdf_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false, unique = true)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private long fileSize;

    private int pageCount;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private String songTitle;

    @Column(length = 1000)
    private String description;

    @Column(name = "cover_image_path")
    private String coverImagePath;

    @Column(nullable = false)
    @Builder.Default
    private int difficulty = 1;

    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
}
