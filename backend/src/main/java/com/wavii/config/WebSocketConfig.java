package com.wavii.config;

import com.wavii.service.ClassChatWebSocketHandler;
import com.wavii.service.ForumChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración de WebSockets para la comunicación en tiempo real.
 * Registra los handlers para chats de clases y foros.
 * 
 * @author eduglezexp
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClassChatWebSocketHandler classChatWebSocketHandler;
    private final ForumChatWebSocketHandler forumChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(classChatWebSocketHandler, "/ws/classes")
                .setAllowedOriginPatterns("*");
        registry.addHandler(forumChatWebSocketHandler, "/ws/forums")
                .setAllowedOriginPatterns("*");
    }
}
