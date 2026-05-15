package com.wavii.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"pdf_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_id", nullable = false)
    private PdfDocument pdf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime likedAt;
}
