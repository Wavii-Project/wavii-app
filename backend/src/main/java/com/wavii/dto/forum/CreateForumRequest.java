package com.wavii.dto.forum;

import com.wavii.model.enums.ForumCategory;

public record CreateForumRequest(
        String name,
        String description,
        ForumCategory category,
        String coverImageUrl,
        String city
) {
}
