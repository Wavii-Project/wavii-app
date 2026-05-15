package com.wavii.controller;

import com.wavii.dto.news.NewsArticleDto;
import com.wavii.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    /**
     * GET /api/news
     *
     * Devuelve articulos de noticias de musica desde NewsData.io.
     * Los resultados se cachean 30 minutos para no consumir creditos innecesarios.
     *
     * Parametros opcionales:
     * - q:        terminos de busqueda (por defecto "music")
     * - language: "es", "en" o vacio para ambos
     * - size:     numero de articulos (maximo 10, por defecto 6)
     *
     * Este endpoint es publico: no requiere autenticacion para que la seccion
     * de noticias del Home funcione aunque el usuario no este logado.
     */
    @GetMapping
    public ResponseEntity<List<NewsArticleDto>> getNews(
            @RequestParam(defaultValue = "music") String q,
            @RequestParam(required = false)        String language,
            @RequestParam(defaultValue = "6")      int size
    ) {
        List<NewsArticleDto> articles = newsService.fetchMusicNews(q, language, size);
        return ResponseEntity.ok(articles);
    }
}
