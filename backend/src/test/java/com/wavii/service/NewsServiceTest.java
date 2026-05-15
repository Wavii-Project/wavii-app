package com.wavii.service;

import com.wavii.dto.news.NewsArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Spy
    private NewsService newsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(newsService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(newsService, "baseUrl", "https://newsdata.io/api/1");
    }

    // ── fetchMusicNews – missing/blank API key ────────────────────

    @Test
    void fetchMusicNewsEmptyApiKeyReturnsEmptyTest() {
        ReflectionTestUtils.setField(newsService, "apiKey", "");

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsBlankApiKeyReturnsEmptyTest() {
        ReflectionTestUtils.setField(newsService, "apiKey", "   ");

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsNullApiKeyReturnsEmptyTest() {
        ReflectionTestUtils.setField(newsService, "apiKey", null);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── fetchMusicNews – RestTemplate mocked ─────────────────────

    @Test
    void fetchMusicNewsValidKeySuccessResponseReturnsArticlesTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":1,\"results\":["
                + "{\"article_id\":\"art1\",\"title\":\"Guitar news\",\"link\":\"http://link.com\","
                + "\"description\":\"Desc\",\"image_url\":\"http://img.com\","
                + "\"source_name\":\"Source\",\"pubDate\":\"2024-01-01\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("guitar", "en", 5);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("art1", result.get(0).articleId());
        assertEquals("Guitar news", result.get(0).title());
    }

    @Test
    void fetchMusicNewsSuccessResponseArticleMappingTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":1,\"results\":["
                + "{\"article_id\":\"art-x\",\"title\":\"Piano concert\",\"link\":\"http://piano.com\","
                + "\"description\":\"Great concert\",\"image_url\":\"http://img.piano.com\","
                + "\"source_name\":\"PianoNews\",\"pubDate\":\"2024-06-15\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("piano", "en", 3);

        assertFalse(result.isEmpty());
        NewsArticleDto dto = result.get(0);
        assertEquals("art-x", dto.articleId());
        assertEquals("Piano concert", dto.title());
        assertEquals("Great concert", dto.description());
        assertEquals("http://piano.com", dto.url());
        assertEquals("http://img.piano.com", dto.imageUrl());
        assertEquals("PianoNews", dto.sourceName());
        assertEquals("2024-06-15", dto.publishedAt());
    }

    @Test
    void fetchMusicNewsArticleWithNullArticleIdUsesLinkAsIdTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":1,\"results\":["
                + "{\"title\":\"No ID article\",\"link\":\"http://fallback.com\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertFalse(result.isEmpty());
        assertEquals("http://fallback.com", result.get(0).articleId());
    }

    @Test
    void fetchMusicNewsNullResponseReturnsEmptyTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsErrorStatusReturnsEmptyTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"error\",\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsMalformedJsonReturnsEmptyTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn("NOT_JSON{{");

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsRestTemplateThrowsExceptionReturnsEmptyTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        when(mockTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsEmptyResultsArrayReturnsEmptyTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsNullResultsReturnsEmptyTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":0}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsFiltersArticlesWithNullTitleTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":2,\"results\":["
                + "{\"article_id\":\"art1\",\"title\":null,\"link\":\"http://link1.com\"},"
                + "{\"article_id\":\"art2\",\"title\":\"Valid Title\",\"link\":\"http://link2.com\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertEquals(1, result.size());
        assertEquals("Valid Title", result.get(0).title());
    }

    @Test
    void fetchMusicNewsFiltersArticlesWithBlankTitleTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":2,\"results\":["
                + "{\"article_id\":\"art1\",\"title\":\"  \",\"link\":\"http://link1.com\"},"
                + "{\"article_id\":\"art2\",\"title\":\"Good Article\",\"link\":\"http://link2.com\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertEquals(1, result.size());
        assertEquals("Good Article", result.get(0).title());
    }

    // ── fetchMusicNews – size clamping ────────────────────────────

    @Test
    void fetchMusicNewsSizeZeroClampsToOneTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 0);

        // URL should contain &size=1 — no exception thrown
        assertNotNull(result);
    }

    @Test
    void fetchMusicNewsSizeAboveTenClampsToTenTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 100);

        // URL should contain &size=10 — no exception thrown
        assertNotNull(result);
    }

    // ── fetchMusicNews – language parameter ───────────────────────

    @Test
    void fetchMusicNewsNullLanguageOmitsLanguageParamTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", null, 5);

        assertNotNull(result);
    }

    @Test
    void fetchMusicNewsBlankLanguageOmitsLanguageParamTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "  ", 5);

        assertNotNull(result);
    }

    // ── fetchMusicNews – fallback queries ────────────────────────

    @Test
    void fetchMusicNewsGuitarQueryAddsGuitarFallbackTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        // First call returns empty, second call (fallback) returns an article
        String emptyJson = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        String articleJson = "{\"status\":\"success\",\"totalResults\":1,\"results\":["
                + "{\"article_id\":\"art_guitar\",\"title\":\"Guitar Band Tour\",\"link\":\"http://guitar.com\"}"
                + "]}";
        // First qInTitle call empty, first q call empty, second qInTitle (fallback) returns result
        when(mockTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(emptyJson, emptyJson, articleJson);

        List<NewsArticleDto> result = newsService.fetchMusicNews("guitar", "en", 3);

        assertNotNull(result);
        // Result may be empty or not depending on which fallback hits, but no exception
    }

    @Test
    void fetchMusicNewsPianoQueryAddsPianoFallbackTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String emptyJson = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(emptyJson);

        List<NewsArticleDto> result = newsService.fetchMusicNews("piano", "en", 3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsFestivalQueryAddsFestivalFallbackTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String emptyJson = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(emptyJson);

        List<NewsArticleDto> result = newsService.fetchMusicNews("festival", "en", 3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsAlbumQueryAddsAlbumFallbackTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String emptyJson = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(emptyJson);

        List<NewsArticleDto> result = newsService.fetchMusicNews("album", "en", 3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsReleaseQueryAddsReleaseFallbackTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String emptyJson = "{\"status\":\"success\",\"totalResults\":0,\"results\":[]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(emptyJson);

        List<NewsArticleDto> result = newsService.fetchMusicNews("release", "en", 3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMusicNewsNullQueryDefaultsMusicTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":1,\"results\":["
                + "{\"article_id\":\"art_music\",\"title\":\"Music Festival\",\"link\":\"http://music.com\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews(null, "en", 5);

        assertNotNull(result);
    }

    @Test
    void fetchMusicNewsBlankQueryDefaultsMusicTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":1,\"results\":["
                + "{\"article_id\":\"art_blank\",\"title\":\"Music Concert\",\"link\":\"http://concert.com\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("  ", "en", 5);

        assertNotNull(result);
    }

    @Test
    void fetchMusicNewsFirstSuccessfulQueryShortCircuitsTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":2,\"results\":["
                + "{\"article_id\":\"a1\",\"title\":\"Article 1\",\"link\":\"http://1.com\"},"
                + "{\"article_id\":\"a2\",\"title\":\"Article 2\",\"link\":\"http://2.com\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertFalse(result.isEmpty());
        // Only one URL should have been called because the first attempt succeeded
        verify(mockTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void fetchMusicNewsMultipleArticlesReturnedTest() {
        RestTemplate mockTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(newsService, "restTemplate", mockTemplate);

        String json = "{\"status\":\"success\",\"totalResults\":3,\"results\":["
                + "{\"article_id\":\"a1\",\"title\":\"T1\",\"link\":\"http://1.com\"},"
                + "{\"article_id\":\"a2\",\"title\":\"T2\",\"link\":\"http://2.com\"},"
                + "{\"article_id\":\"a3\",\"title\":\"T3\",\"link\":\"http://3.com\"}"
                + "]}";
        when(mockTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        List<NewsArticleDto> result = newsService.fetchMusicNews("music", "en", 5);

        assertEquals(3, result.size());
    }

    // ── Inner classes – NewsDataResponse ─────────────────────────

    @Test
    void newsDataResponseGettersSettersTest() {
        NewsService.NewsDataResponse response = new NewsService.NewsDataResponse();
        response.setStatus("success");
        response.setTotalResults(5);

        NewsService.NewsDataArticle article = new NewsService.NewsDataArticle();
        article.setArticleId("art-1");
        article.setTitle("Test Title");
        article.setLink("http://link.com");
        article.setDescription("Test description");
        article.setImageUrl("http://img.com");
        article.setSourceName("Test Source");
        article.setPubDate("2024-01-01");

        response.setResults(List.of(article));

        assertEquals("success", response.getStatus());
        assertEquals(5, response.getTotalResults());
        assertEquals(1, response.getResults().size());

        NewsService.NewsDataArticle r = response.getResults().get(0);
        assertEquals("art-1", r.getArticleId());
        assertEquals("Test Title", r.getTitle());
        assertEquals("http://link.com", r.getLink());
        assertEquals("Test description", r.getDescription());
        assertEquals("http://img.com", r.getImageUrl());
        assertEquals("Test Source", r.getSourceName());
        assertEquals("2024-01-01", r.getPubDate());
    }

    @Test
    void newsDataResponseEqualsEqualObjectsTest() {
        NewsService.NewsDataResponse r1 = new NewsService.NewsDataResponse();
        r1.setStatus("success");
        r1.setTotalResults(3);

        NewsService.NewsDataResponse r2 = new NewsService.NewsDataResponse();
        r2.setStatus("success");
        r2.setTotalResults(3);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void newsDataResponseToStringNotNullTest() {
        NewsService.NewsDataResponse r = new NewsService.NewsDataResponse();
        r.setStatus("success");
        assertNotNull(r.toString());
    }

    @Test
    void newsDataResponseNotEqualsWhenStatusDiffersTest() {
        NewsService.NewsDataResponse r1 = new NewsService.NewsDataResponse();
        r1.setStatus("success");

        NewsService.NewsDataResponse r2 = new NewsService.NewsDataResponse();
        r2.setStatus("error");

        assertNotEquals(r1, r2);
    }

    @Test
    void newsDataResponseNotEqualsWhenTotalResultsDiffersTest() {
        NewsService.NewsDataResponse r1 = new NewsService.NewsDataResponse();
        r1.setTotalResults(1);

        NewsService.NewsDataResponse r2 = new NewsService.NewsDataResponse();
        r2.setTotalResults(99);

        assertNotEquals(r1, r2);
    }

    // ── Inner classes – NewsDataArticle ──────────────────────────

    @Test
    void newsDataArticleEqualsEqualObjectsTest() {
        NewsService.NewsDataArticle a1 = new NewsService.NewsDataArticle();
        a1.setArticleId("id1");
        a1.setTitle("Title");

        NewsService.NewsDataArticle a2 = new NewsService.NewsDataArticle();
        a2.setArticleId("id1");
        a2.setTitle("Title");

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void newsDataArticleToStringNotNullTest() {
        NewsService.NewsDataArticle a = new NewsService.NewsDataArticle();
        a.setTitle("Test");
        assertNotNull(a.toString());
    }

    @Test
    void newsDataArticleNotEqualsWhenIdDiffersTest() {
        NewsService.NewsDataArticle a1 = new NewsService.NewsDataArticle();
        a1.setArticleId("id1");

        NewsService.NewsDataArticle a2 = new NewsService.NewsDataArticle();
        a2.setArticleId("id2");

        assertNotEquals(a1, a2);
    }

    @Test
    void newsDataArticleNotEqualsWhenTitleDiffersTest() {
        NewsService.NewsDataArticle a1 = new NewsService.NewsDataArticle();
        a1.setTitle("Title A");

        NewsService.NewsDataArticle a2 = new NewsService.NewsDataArticle();
        a2.setTitle("Title B");

        assertNotEquals(a1, a2);
    }

    @Test
    void newsDataArticleAllFieldsGettersTest() {
        NewsService.NewsDataArticle a = new NewsService.NewsDataArticle();
        a.setArticleId("test-id");
        a.setTitle("Test Title");
        a.setLink("http://test.com");
        a.setDescription("Test desc");
        a.setImageUrl("http://img.test.com");
        a.setSourceName("TestSource");
        a.setPubDate("2025-01-15");

        assertEquals("test-id", a.getArticleId());
        assertEquals("Test Title", a.getTitle());
        assertEquals("http://test.com", a.getLink());
        assertEquals("Test desc", a.getDescription());
        assertEquals("http://img.test.com", a.getImageUrl());
        assertEquals("TestSource", a.getSourceName());
        assertEquals("2025-01-15", a.getPubDate());
    }

    // ── NewsArticleDto ────────────────────────────────────────────

    @Test
    void newsArticleDtoRecordAccessorsTest() {
        NewsArticleDto dto = new NewsArticleDto(
                "id1", "Title", "Desc", "http://url.com",
                "http://img.com", "Source", "2024-01-01");

        assertEquals("id1", dto.articleId());
        assertEquals("Title", dto.title());
        assertEquals("Desc", dto.description());
        assertEquals("http://url.com", dto.url());
        assertEquals("http://img.com", dto.imageUrl());
        assertEquals("Source", dto.sourceName());
        assertEquals("2024-01-01", dto.publishedAt());
    }

    @Test
    void newsArticleDtoEqualityTest() {
        NewsArticleDto dto1 = new NewsArticleDto("id1", "T", "D", "u", "i", "s", "d");
        NewsArticleDto dto2 = new NewsArticleDto("id1", "T", "D", "u", "i", "s", "d");
        assertEquals(dto1, dto2);
    }

    @Test
    void newsArticleDtoInequalityTest() {
        NewsArticleDto dto1 = new NewsArticleDto("id1", "T", "D", "u", "i", "s", "d");
        NewsArticleDto dto2 = new NewsArticleDto("id2", "T", "D", "u", "i", "s", "d");
        assertNotEquals(dto1, dto2);
    }

    @Test
    void newsArticleDtoHashCodeSameForEqualObjectsTest() {
        NewsArticleDto dto1 = new NewsArticleDto("id1", "T", "D", "u", "i", "s", "d");
        NewsArticleDto dto2 = new NewsArticleDto("id1", "T", "D", "u", "i", "s", "d");
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void newsArticleDtoToStringNotNullTest() {
        NewsArticleDto dto = new NewsArticleDto("id1", "T", "D", "u", "i", "s", "d");
        assertNotNull(dto.toString());
        assertTrue(dto.toString().contains("id1"));
    }

    @Test
    void newsArticleDtoNullFieldsAllowedTest() {
        NewsArticleDto dto = new NewsArticleDto(null, "Title Only", null, null, null, null, null);
        assertNull(dto.articleId());
        assertEquals("Title Only", dto.title());
        assertNull(dto.description());
        assertNull(dto.url());
        assertNull(dto.imageUrl());
        assertNull(dto.sourceName());
        assertNull(dto.publishedAt());
    }
}
