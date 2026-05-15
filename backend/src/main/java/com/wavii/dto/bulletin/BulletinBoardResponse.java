package com.wavii.dto.bulletin;

import java.util.List;

public record BulletinBoardResponse(
        List<BulletinTeacherResponse> teachers,
        boolean hasFullAccess,
        boolean canPublish,
        int visibleLimit,
        int totalCount,
        int hiddenCount,
        String requiredPlan
) {}
