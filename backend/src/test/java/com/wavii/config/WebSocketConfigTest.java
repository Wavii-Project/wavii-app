package com.wavii.config;

import com.wavii.service.ClassChatWebSocketHandler;
import com.wavii.service.DirectMessageWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private ClassChatWebSocketHandler classChatWebSocketHandler;

    @Mock
    private com.wavii.service.ForumChatWebSocketHandler forumChatWebSocketHandler;

    @Mock
    private DirectMessageWebSocketHandler directMessageWebSocketHandler;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Test
    void registerWebSocketHandlersRegistersHandlerAtCorrectPathTest() {
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(any(), any(String[].class)))
                .thenReturn(registration);
        when(registration.setAllowedOriginPatterns(any(String[].class))).thenReturn(registration);

        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registry).addHandler(classChatWebSocketHandler, "/ws/classes");
        verify(registry).addHandler(forumChatWebSocketHandler, "/ws/forums");
        verify(registry).addHandler(directMessageWebSocketHandler, "/ws/direct");
        verify(registration, times(3)).setAllowedOriginPatterns("*");
    }

    @Test
    void registerWebSocketHandlersAllowsAllOriginsTest() {
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(any(), any(String[].class))).thenReturn(registration);
        when(registration.setAllowedOriginPatterns(any(String[].class))).thenReturn(registration);

        webSocketConfig.registerWebSocketHandlers(registry);

        verify(registration, times(3)).setAllowedOriginPatterns("*");
    }

    @Test
    void webSocketConfigHandlerIsNotNullTest() {
        assertNotNull(classChatWebSocketHandler);
    }
}
