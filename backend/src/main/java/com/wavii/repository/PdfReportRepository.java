package com.wavii.repository;

import com.wavii.model.PdfReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PdfReportRepository extends JpaRepository<PdfReport, Long> {

    @Modifying
    @Query("DELETE FROM PdfReport r WHERE r.pdfDocument.id = :pdfId")
    void deleteByPdfDocumentId(@Param("pdfId") Long pdfId);
}
