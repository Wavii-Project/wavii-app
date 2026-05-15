package com.wavii.service;

import com.wavii.dto.dm.DirectMessageDto;
import com.wavii.model.DirectMessage;
import com.wavii.model.User;
import com.wavii.repository.DirectMessageRepository;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DirectMessageDto> getConversation(User me, UUID userId) {
        User other = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return directMessageRepository.findConversation(me, other)
                .stream()
                .map(DirectMessageDto::from)
                .toList();
    }

    @Transactional
    public DirectMessageDto sendMessage(User me, UUID userId, String rawContent) {
        if (me == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion no valida");
        }
        if (me.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes enviarte mensajes a ti mismo.");
        }

        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!receiver.isAcceptsMessages()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este usuario ha desactivado los mensajes directos.");
        }

        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isBlank() || content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no puede estar vacio ni superar los 2000 caracteres.");
        }

        DirectMessage saved = directMessageRepository.save(DirectMessage.builder()
                .sender(me)
                .receiver(receiver)
                .content(content)
                .build());

        return DirectMessageDto.from(saved);
    }
}
