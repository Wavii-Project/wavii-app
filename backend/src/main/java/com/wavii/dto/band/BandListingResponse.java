package com.wavii.dto.band;

import java.util.List;

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
