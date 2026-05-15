package com.wavii.repository;

import com.wavii.model.PdfDocument;
import com.wavii.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad PdfDocument.
 * Gestiona el almacenamiento y búsqueda de tablaturas en formato PDF.
 * 
 * @author danielrguezh
 */
@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long>, JpaSpecificationExecutor<PdfDocument> {

    /** Lista todos los PDFs de un propietario ordenados por fecha de subida. */
    List<PdfDocument> findByOwnerOrderByUploadedAtDesc(User owner);

    /** Busca un PDF por ID y propietario. */
    Optional<PdfDocument> findByIdAndOwner(Long id, User owner);

    /** Cuenta el número de PDFs subidos por un usuario. */
    long countByOwnerId(UUID ownerId);

    /** Busca tablaturas públicas de un usuario cargando los datos del dueño. */
    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE p.owner.id = :ownerId ORDER BY p.uploadedAt DESC")
    List<PdfDocument> findPublicByOwnerIdOrderByUploadedAtDesc(@Param("ownerId") UUID ownerId);

    /** Obtiene el feed general de tablaturas con paginación. */
    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner")
    List<PdfDocument> findAllPublicFeed(Pageable pageable);

    /** Filtra el feed por nivel de dificultad. */
    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE p.difficulty = :difficulty")
    List<PdfDocument> findPublicFeedByDifficulty(@Param("difficulty") int difficulty, Pageable pageable);

    /** Filtra el feed por título de canción. */
    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE LOWER(p.songTitle) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<PdfDocument> findPublicFeedBySearch(@Param("search") String search, Pageable pageable);

    /** Filtra el feed por título y dificultad simultáneamente. */
    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE LOWER(p.songTitle) LIKE LOWER(CONCAT('%', :search, '%')) AND p.difficulty = :difficulty")
    List<PdfDocument> findPublicFeedBySearchAndDifficulty(@Param("search") String search, @Param("difficulty") int difficulty, Pageable pageable);

    /** Obtiene todas las tablaturas de una dificultad específica incluyendo al dueño. */
    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE p.difficulty = :difficulty")
    List<PdfDocument> findAllByDifficultyWithOwner(@Param("difficulty") int difficulty);

    /** Incrementa de forma atómica el contador de likes de una tablatura. */
    @Modifying
    @Query("UPDATE PdfDocument p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    /** Decrementa de forma atómica el contador de likes de una tablatura. */
    @Modifying
    @Query("UPDATE PdfDocument p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);
}
