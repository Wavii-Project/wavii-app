package com.wavii.model;

import com.wavii.model.enums.ListingType;
import com.wavii.model.enums.MusicalGenre;
import com.wavii.model.enums.MusicianRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa un anuncio de banda o músico en el marketplace.
 * Contiene información sobre el tipo de anuncio, género musical, ciudad y roles buscados.
 * 
 * @author danielrguezh
 */
@Entity
@Table(name = "band_listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BandListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MusicalGenre genre;

    @Column(nullable = false)
    private String city;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "band_listing_roles", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "role", nullable = false)
    @Builder.Default
    private List<MusicianRole> roles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(length = 200)
    private String contactInfo;

    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "band_listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
