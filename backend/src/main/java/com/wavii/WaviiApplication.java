package com.wavii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de la aplicación Wavii.
 * Punto de entrada de Spring Boot que habilita procesamiento asíncrono, caché y tareas programadas.
 * 
 * @author eduglezexp
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
public class WaviiApplication {

    /**
     * Punto de entrada de la aplicación Spring Boot.
     * 
     * Este método inicia el contenedor de Spring leyendo la configuración y
     * ejecutando la clase principal indicada ({@link WaviiApplication}) con los
     * argumentos proporcionados por la línea de comandos.
     * 
     * @param args argumentos recibidos al ejecutar la aplicación.
     */
    public static void main(String[] args) {
        SpringApplication.run(WaviiApplication.class, args);
    }
}
