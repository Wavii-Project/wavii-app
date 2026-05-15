package com.wavii.controller;

import com.wavii.dto.dm.DirectMessageDto;
import com.wavii.model.User;
import com.wavii.service.ChatRealtimeBroadcaster;
import com.wavii.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages/direct")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final ChatRealtimeBroadcaster chatRealtimeBroadcaster;

    @GetMapping("/{userId}")
    public ResponseEntity<List<DirectMessageDto>> getConversation(
            @AuthenticationPrincipal User me,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(directMessageService.getConversation(me, userId));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<DirectMessageDto> sendMessage(
            @AuthenticationPrincipal User me,
            @PathVariable UUID userId,
            @RequestBody Map<String, String> body
    ) {
        DirectMessageDto saved = directMessageService.sendMessage(me, userId, body.get("content"));
        chatRealtimeBroadcaster.broadcast(
                chatRealtimeBroadcaster.directRoom(me.getId(), userId),
                saved
        );
        return ResponseEntity.status(201).body(saved);
    }
}
