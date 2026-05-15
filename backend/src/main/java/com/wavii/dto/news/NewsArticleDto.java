package com.wavii.dto.news;

/**
 * DTO que representa un articulo de noticias devuelto al frontend.
 * Los campos se mapean desde la respuesta de NewsData.io y se normalizan
 * para que el frontend no dependa del formato externo.
 */
public record NewsArticleDto(
        String articleId,
        String title,
        String description,
        String url,
        String imageUrl,
        String sourceName,
        String publishedAt
) {}
