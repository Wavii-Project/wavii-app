package com.wavii.dto.dm;

import com.wavii.model.DirectMessage;

import java.util.UUID;

public record DirectMessageDto(
        UUID id,
        UUID senderId,
        String senderName,
        String content,
        String createdAt
) {
    public static DirectMessageDto from(DirectMessage dm) {
        return new DirectMessageDto(
                dm.getId(),
                dm.getSender().getId(),
                dm.getSender().getName(),
                dm.getContent(),
                dm.getCreatedAt().toString()
        );
    }
}
