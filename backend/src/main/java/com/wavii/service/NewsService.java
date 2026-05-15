package com.wavii.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavii.dto.news.NewsArticleDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NewsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${newsdata.api-key}")
    private String apiKey;

    @Value("${newsdata.base-url}")
    private String baseUrl;

    /**
     * Obtiene artículos de noticias musicales desde NewsData.io con sistema de caché.
     * Si la búsqueda principal no devuelve resultados, intenta con términos alternativos.
     * 
     * @param query Término de búsqueda (ej. "guitarra", "rock").
     * @param language Código de idioma (ej. "es", "en").
     * @param size Cantidad máxima de artículos a recuperar.
     * @return Lista de artículos de noticias encontrados.
     */
    @Cacheable(
            value = "news",
            key = "T(String).format('%s|%s|%s', #query ?: '', #language ?: '', #size)",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<NewsArticleDto> fetchMusicNews(String query, String language, int size) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("NEWSDATA_API_KEY no configurada");
            return Collections.emptyList();
        }

        int safeSize = Math.min(Math.max(size, 1), 10);
        List<String> fallbackQueries = buildFallbackQueries(query);

        try {
            for (String currentQuery : fallbackQueries) {
                List<NewsArticleDto> articles = requestNews(currentQuery, language, safeSize, true);
                if (!articles.isEmpty()) {
                    return articles;
                }

                articles = requestNews(currentQuery, language, safeSize, false);
                if (!articles.isEmpty()) {
                    return articles;
                }
            }

            log.info("NewsData.io no devolvio resultados para query={} ni para sus alternativas", query);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error llamando a NewsData.io: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<NewsArticleDto> requestNews(String query, String language, int size, boolean titleOnly) throws Exception {
        String encodedQuery = UriUtils.encodeQueryParam(query, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(baseUrl)
                .append("/latest")
                .append("?apikey=").append(apiKey)
                .append(titleOnly ? "&qInTitle=" : "&q=").append(encodedQuery)
                .append("&size=").append(size);

        if (language != null && !language.isBlank()) {
            sb.append("&language=").append(language);
        }

        String url = sb.toString();
        log.info(
                "Llamando a NewsData.io con {}: {}",
                titleOnly ? "qInTitle" : "q",
                url.replace(apiKey, "***")
        );

        String raw = restTemplate.getForObject(url, String.class);
        if (raw == null) {
            log.warn("NewsData.io devolvio respuesta nula para query={}", query);
            return Collections.emptyList();
        }

        NewsDataResponse response = new ObjectMapper().readValue(raw, NewsDataResponse.class);

        if (!"success".equals(response.getStatus())) {
            log.warn("NewsData.io devolvio status={}: {}", response.getStatus(), raw);
            return Collections.emptyList();
        }

        if (response.getResults() == null || response.getResults().isEmpty()) {
            log.info("NewsData.io no devolvio resultados para query={} usando {}", query, titleOnly ? "qInTitle" : "q");
            return Collections.emptyList();
        }

        List<NewsArticleDto> articles = response.getResults().stream()
                .filter(r -> r.getTitle() != null && !r.getTitle().isBlank())
                .map(r -> new NewsArticleDto(
                        r.getArticleId() != null ? r.getArticleId() : r.getLink(),
                        r.getTitle(),
                        r.getDescription(),
                        r.getLink(),
                        r.getImageUrl(),
                        r.getSourceName(),
                        r.getPubDate()
                ))
                .collect(Collectors.toList());

        log.info(
                "NewsData.io: {} articulos obtenidos para query={} usando {}",
                articles.size(),
                query,
                titleOnly ? "qInTitle" : "q"
        );
        return articles;
    }

    private List<String> buildFallbackQueries(String query) {
        String normalizedQuery = (query != null && !query.isBlank()) ? query.trim() : "music";
        String lowerQuery = normalizedQuery.toLowerCase(Locale.ROOT);
        Set<String> queries = new LinkedHashSet<>();
        queries.add(normalizedQuery);

        if (lowerQuery.contains("guitar")) {
            queries.add("guitar OR guitarist OR band OR music");
        } else if (lowerQuery.contains("piano")) {
            queries.add("piano OR pianist OR music");
        } else if (lowerQuery.contains("festival")) {
            queries.add("music festival OR concert OR live music");
        } else if (lowerQuery.contains("album") || lowerQuery.contains("release")) {
            queries.add("album OR single OR artist OR music");
        }

        queries.add("music OR musician OR band OR singer OR album OR song OR concert OR festival");
        queries.add("music");

        return new ArrayList<>(queries);
    }

    // ── Clases internas para deserializar la respuesta de NewsData.io ──

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsDataResponse {
        private String status;
        @JsonProperty("totalResults")
        private int totalResults;
        private List<NewsDataArticle> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewsDataArticle {
        @JsonProperty("article_id")
        private String articleId;

        private String title;
        private String link;
        private String description;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("source_name")
        private String sourceName;

        @JsonProperty("pubDate")
        private String pubDate;
    }
}
