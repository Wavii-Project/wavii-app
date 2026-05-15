package com.wavii.dto.forum;

import com.wavii.model.enums.ForumCategory;

public record UpdateForumRequest(
        String description,
        String coverImageUrl,
        ForumCategory category
) {
}
