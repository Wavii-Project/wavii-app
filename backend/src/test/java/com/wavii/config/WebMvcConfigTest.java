package com.wavii.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebMvcConfigTest {

    @Test
    void webMvcConfigInstantiationDoesNotThrowTest() {
        assertDoesNotThrow(WebMvcConfig::new);
    }

    @Test
    void addResourceHandlersRegistersUploadsHandlerTest() throws Exception {
        WebMvcConfig config = new WebMvcConfig();
        Field field = WebMvcConfig.class.getDeclaredField("pdfStoragePath");
        field.setAccessible(true);
        field.set(config, "./uploads/pdfs");

        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/uploads/**")).thenReturn(registration);
        when(registration.addResourceLocations(anyString())).thenReturn(registration);

        assertDoesNotThrow(() -> config.addResourceHandlers(registry));
        verify(registry).addResourceHandler("/uploads/**");
        verify(registration).addResourceLocations(anyString());
    }
}
