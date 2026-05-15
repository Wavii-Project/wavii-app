package com.wavii.dto.forum;

import com.wavii.model.enums.ForumMembershipRole;

public record UpdateForumMemberRoleRequest(
        ForumMembershipRole role
) {
}
