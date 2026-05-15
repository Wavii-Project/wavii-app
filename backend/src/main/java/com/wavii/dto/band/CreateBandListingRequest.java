package com.wavii.dto.band;

import com.wavii.model.enums.ListingType;
import com.wavii.model.enums.MusicalGenre;
import com.wavii.model.enums.MusicianRole;

import java.util.List;

/**
 * DTO que representa una solicitud para crear un nuevo anuncio de banda.
 */
public record CreateBandListingRequest(
        String title,
        String description,
        ListingType type,
        MusicalGenre genre,
        String city,
        List<MusicianRole> roles,
        String contactInfo,
        String coverImageUrl,
        List<String> imageUrls
) {}
