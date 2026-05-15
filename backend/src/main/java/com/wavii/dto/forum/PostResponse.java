package com.wavii.dto.forum;

public record PostResponse(
        String id,
        String content,
        String authorId,
        String authorName,
        String authorAvatarUrl,
        String createdAt
) {
}
