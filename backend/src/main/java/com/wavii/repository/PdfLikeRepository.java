package com.wavii.repository;

import com.wavii.model.PdfLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PdfLikeRepository extends JpaRepository<PdfLike, Long> {
    boolean existsByPdfIdAndUserId(Long pdfId, UUID userId);
    Optional<PdfLike> findByPdfIdAndUserId(Long pdfId, UUID userId);

    @Query("SELECT l.pdf.id FROM PdfLike l WHERE l.user.id = :userId AND l.pdf.id IN :pdfIds")
    List<Long> findLikedPdfIds(@Param("userId") UUID userId, @Param("pdfIds") List<Long> pdfIds);

    @Modifying
    @Query("DELETE FROM PdfLike l WHERE l.pdf.id = :pdfId")
    void deleteByPdfId(@Param("pdfId") Long pdfId);
}
