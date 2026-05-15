package com.wavii.controller;

import com.wavii.dto.pdf.PdfResponseDto;
import com.wavii.model.PdfDocument;
import com.wavii.model.User;
import com.wavii.service.PdfStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfControllerTest {

    @Mock private PdfStorageService pdfStorageService;

    @InjectMocks private PdfController controller;

    private User user;
    private PdfResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");

        sampleDto = new PdfResponseDto(
                1L, "test.pdf", "stored.pdf",
                1024L, 5, LocalDateTime.now(), "Test Song", null, null,
                2, 3, false, "Test User", user.getId());
    }

    // ─── upload ──────────────────────────────────────────────────

    @Test
    void uploadValidPdfReturns201Test() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());
        when(pdfStorageService.save(any(), any(), eq(user), eq("Song"), isNull(), eq(2))).thenReturn(sampleDto);

        ResponseEntity<PdfResponseDto> result = controller.upload(file, "Song", null, null, 2, user);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(sampleDto, result.getBody());
    }

    @Test
    void uploadIllegalArgumentReturns400Test() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "not a pdf".getBytes());
        when(pdfStorageService.save(any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("Solo PDF"));

        ResponseEntity<PdfResponseDto> result = controller.upload(file, null, null, null, 1, user);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void uploadIOExceptionReturns500Test() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]);
        when(pdfStorageService.save(any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Storage error"));

        ResponseEntity<PdfResponseDto> result = controller.upload(file, null, null, null, 1, user);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    // ─── publicFeed ──────────────────────────────────────────────

    @Test
    void publicFeedNoFiltersReturnsOkTest() {
        when(pdfStorageService.getPublicFeed(null, null, "NEWEST", user)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<PdfResponseDto>> result = controller.publicFeed(null, null, "NEWEST", user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void publicFeedWithFiltersReturnsOkTest() {
        when(pdfStorageService.getPublicFeed("rock", 2, "NEWEST", user)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<PdfResponseDto>> result = controller.publicFeed("rock", 2, "NEWEST", user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(pdfStorageService).getPublicFeed("rock", 2, "NEWEST", user);
    }

    @Test
    void publicFeedEmptyReturnsOkEmptyTest() {
        when(pdfStorageService.getPublicFeed(null, null, "NEWEST", user)).thenReturn(List.of());

        ResponseEntity<List<PdfResponseDto>> result = controller.publicFeed(null, null, "NEWEST", user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ─── list ────────────────────────────────────────────────────

    @Test
    void listReturnsUserDocsTest() {
        when(pdfStorageService.listByUser(user)).thenReturn(List.of(sampleDto));

        ResponseEntity<List<PdfResponseDto>> result = controller.list(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void listEmptyReturnsOkEmptyTest() {
        when(pdfStorageService.listByUser(user)).thenReturn(List.of());

        ResponseEntity<List<PdfResponseDto>> result = controller.list(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ─── download ────────────────────────────────────────────────

    @Test
    void downloadExistingReturnsResourceTest() throws Exception {
        Resource resource = mock(Resource.class);
        PdfDocument doc = PdfDocument.builder()
                .id(1L).originalName("test.pdf").fileName("stored.pdf")
                .filePath("/path/stored.pdf").owner(user).build();

        when(pdfStorageService.loadAsResource(1L)).thenReturn(resource);
        when(pdfStorageService.findById(1L)).thenReturn(doc);

        ResponseEntity<Resource> result = controller.download(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    void downloadNotFoundReturns404Test() throws Exception {
        when(pdfStorageService.loadAsResource(99L)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<Resource> result = controller.download(99L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    // ─── like ────────────────────────────────────────────────────

    @Test
    void likeSuccessReturnsOkTest() {
        PdfResponseDto liked = new PdfResponseDto(
                1L, "test.pdf", "stored.pdf", 1024L, 5,
                LocalDateTime.now(), "Song", null, null, 2, 4, true, "User", user.getId());
        when(pdfStorageService.like(1L, user)).thenReturn(liked);

        ResponseEntity<PdfResponseDto> result = controller.like(1L, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().likedByMe());
    }

    @Test
    void likeNotFoundReturns404Test() {
        when(pdfStorageService.like(99L, user)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<PdfResponseDto> result = controller.like(99L, user);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    // ─── unlike ──────────────────────────────────────────────────

    @Test
    void unlikeSuccessReturnsOkTest() {
        when(pdfStorageService.unlike(1L, user)).thenReturn(sampleDto);

        ResponseEntity<PdfResponseDto> result = controller.unlike(1L, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(result.getBody().likedByMe());
    }

    @Test
    void unlikeNotFoundReturns404Test() {
        when(pdfStorageService.unlike(99L, user)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<PdfResponseDto> result = controller.unlike(99L, user);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    // ─── delete ──────────────────────────────────────────────────

    @Test
    void deleteOwnerReturns204Test() throws Exception {
        doNothing().when(pdfStorageService).delete(1L, user);

        ResponseEntity<Void> result = controller.delete(1L, user);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(pdfStorageService).delete(1L, user);
    }

    @Test
    void deleteNotOwnerReturns403Test() throws Exception {
        doThrow(new SecurityException("No autorizado")).when(pdfStorageService).delete(1L, user);

        ResponseEntity<Void> result = controller.delete(1L, user);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void deleteNotFoundReturns404Test() throws Exception {
        doThrow(new RuntimeException("Not found")).when(pdfStorageService).delete(99L, user);

        ResponseEntity<Void> result = controller.delete(99L, user);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }
}
