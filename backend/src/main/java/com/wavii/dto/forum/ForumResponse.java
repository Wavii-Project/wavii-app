package com.wavii.dto.forum;

public record ForumResponse(
        String id,
        String name,
        String description,
        String category,
        int memberCount,
        boolean joined,
        String coverImageUrl,
        String creatorId,
        String creatorName,
        String city,
        String createdAt,
        int likeCount,
        boolean likedByMe,
        String currentUserRole,
        java.util.List<ForumMemberResponse> members
) {
}
