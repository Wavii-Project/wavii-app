package com.wavii.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuración para habilitar el procesamiento asíncrono en Spring Boot.
 * Permite el uso de la anotación @Async para tareas en segundo plano.
 * 
 * @author eduglezexp
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
