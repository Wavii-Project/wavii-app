package com.wavii.dto.forum;

public record ForumSummaryResponse(
        String id,
        String name,
        String description,
        String category,
        int memberCount,
        boolean joined,
        String coverImageUrl,
        String creatorName,
        String city,
        int likeCount,
        boolean likedByMe
) {
}
