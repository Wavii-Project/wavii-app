package com.wavii.dto.bulletin;

import java.math.BigDecimal;
import java.util.List;

public record BulletinUpdateRequest(
        String instrument,
        BigDecimal pricePerHour,
        String bio,
        String city,
        Double latitude,
        Double longitude,
        String address,
        String province,
        String contactEmail,
        String contactPhone,
        String instagramUrl,
        String tiktokUrl,
        String youtubeUrl,
        String facebookUrl,
        String bannerImageUrl,
        List<String> placeImageUrls,
        String availabilityPreference,
        String availabilityNotes,
        /** PRESENCIAL | ONLINE | AMBAS */
        String classModality) {
}
