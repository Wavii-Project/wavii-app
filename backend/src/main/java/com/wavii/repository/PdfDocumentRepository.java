package com.wavii.repository;

import com.wavii.model.PdfDocument;
import com.wavii.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {

    List<PdfDocument> findByOwnerOrderByUploadedAtDesc(User owner);
    Optional<PdfDocument> findByIdAndOwner(Long id, User owner);
    long countByOwnerId(UUID ownerId);

    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE p.owner.id = :ownerId ORDER BY p.uploadedAt DESC")
    List<PdfDocument> findPublicByOwnerIdOrderByUploadedAtDesc(@Param("ownerId") UUID ownerId);

    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner")
    List<PdfDocument> findAllPublicFeed(Pageable pageable);

    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE p.difficulty = :difficulty")
    List<PdfDocument> findPublicFeedByDifficulty(@Param("difficulty") int difficulty, Pageable pageable);

    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE LOWER(p.songTitle) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<PdfDocument> findPublicFeedBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE LOWER(p.songTitle) LIKE LOWER(CONCAT('%', :search, '%')) AND p.difficulty = :difficulty")
    List<PdfDocument> findPublicFeedBySearchAndDifficulty(@Param("search") String search, @Param("difficulty") int difficulty, Pageable pageable);

    @Query("SELECT p FROM PdfDocument p JOIN FETCH p.owner WHERE p.difficulty = :difficulty")
    List<PdfDocument> findAllByDifficultyWithOwner(@Param("difficulty") int difficulty);

    @Modifying
    @Query("UPDATE PdfDocument p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PdfDocument p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);
}
