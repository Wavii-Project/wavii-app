package com.wavii.dto.band;

import java.util.List;

/**
 * DTO que contiene la información de un anuncio de banda para ser mostrada en la interfaz.
 * 
 * @author danielrguezh
 */
public record BandListingResponse(
        String id,
        String title,
        String description,
        String type,
        String genre,
        String city,
        List<String> roles,
        String creatorId,
        String creatorName,
        String contactInfo,
        String coverImageUrl,
        List<String> imageUrls,
        String createdAt
) {}
