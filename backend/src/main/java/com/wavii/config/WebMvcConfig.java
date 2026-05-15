package com.wavii.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * pdf.storage.path es "./uploads/pdfs" en dev y "/app/uploads/pdfs" en Docker.
     * Subimos un nivel para obtener la raíz de uploads y servir también las portadas.
     */
    @Value("${pdf.storage.path:./uploads/pdfs}")
    private String pdfStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Sube un nivel desde "uploads/pdfs" → "uploads/"
        Path uploadsRoot = Paths.get(pdfStoragePath).getParent();
        if (uploadsRoot == null) {
            uploadsRoot = Paths.get("uploads");
        }
        String location = "file:" + uploadsRoot.toAbsolutePath() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
