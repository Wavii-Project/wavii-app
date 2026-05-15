package com.wavii.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_document_id", nullable = false)
    private PdfDocument pdfDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 80)
    private String reason;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";
}
