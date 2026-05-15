package com.wavii.controller;

import com.wavii.dto.news.NewsArticleDto;
import com.wavii.service.NewsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock private NewsService newsService;

    @InjectMocks private NewsController controller;

    @Test
    void getNewsDefaultParamsReturnsOkTest() {
        List<NewsArticleDto> articles = List.of(
                new NewsArticleDto("id1", "Title", "Desc", "http://url", null, "Source", "2024-01-01"));
        when(newsService.fetchMusicNews("music", null, 6)).thenReturn(articles);

        ResponseEntity<List<NewsArticleDto>> result = controller.getNews("music", null, 6);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getNewsWithQueryPassesToServiceTest() {
        when(newsService.fetchMusicNews("guitar", "es", 5)).thenReturn(List.of());

        ResponseEntity<List<NewsArticleDto>> result = controller.getNews("guitar", "es", 5);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(newsService).fetchMusicNews("guitar", "es", 5);
    }

    @Test
    void getNewsEmptyResultReturnsOkWithEmptyTest() {
        when(newsService.fetchMusicNews("unknown_topic", null, 6)).thenReturn(List.of());

        ResponseEntity<List<NewsArticleDto>> result = controller.getNews("unknown_topic", null, 6);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    void getNewsWithLanguagePassesLanguageTest() {
        when(newsService.fetchMusicNews("music", "en", 10)).thenReturn(List.of());

        controller.getNews("music", "en", 10);

        verify(newsService).fetchMusicNews("music", "en", 10);
    }

    @Test
    void getNewsMultipleArticlesReturnsAllTest() {
        List<NewsArticleDto> articles = List.of(
                new NewsArticleDto("id1", "T1", "D1", "u1", null, "S1", "d1"),
                new NewsArticleDto("id2", "T2", "D2", "u2", null, "S2", "d2"),
                new NewsArticleDto("id3", "T3", "D3", "u3", null, "S3", "d3")
        );
        when(newsService.fetchMusicNews("music", null, 6)).thenReturn(articles);

        ResponseEntity<List<NewsArticleDto>> result = controller.getNews("music", null, 6);

        assertEquals(3, result.getBody().size());
    }
}
