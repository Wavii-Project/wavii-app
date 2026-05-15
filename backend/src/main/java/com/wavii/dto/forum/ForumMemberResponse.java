package com.wavii.dto.forum;

public record ForumMemberResponse(
        String userId,
        String name,
        String avatarUrl,
        String role,
        String joinedAt
) {
}
