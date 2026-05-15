package com.wavii.service;

import com.wavii.dto.pdf.PdfResponseDto;
import com.wavii.model.PdfDocument;
import com.wavii.model.PdfLike;
import com.wavii.model.User;
import com.wavii.repository.PdfDocumentRepository;
import com.wavii.repository.PdfLikeRepository;
import com.wavii.repository.PdfReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfStorageServiceTest {

    @Mock private PdfDocumentRepository repository;
    @Mock private PdfLikeRepository likeRepository;
    @Mock private PdfReportRepository reportRepository;
    @Mock private MultipartFile multipartFile;

    @InjectMocks private PdfStorageService service;

    private User owner;
    private PdfDocument doc;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "storagePath", System.getProperty("java.io.tmpdir") + "/wavii-test-pdfs");

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setName("Owner User");

        doc = PdfDocument.builder()
                .id(1L)
                .originalName("test.pdf")
                .fileName("stored.pdf")
                .filePath(System.getProperty("java.io.tmpdir") + "/wavii-test-pdfs/stored.pdf")
                .fileSize(1024L)
                .pageCount(5)
                .uploadedAt(LocalDateTime.now())
                .songTitle("Test Song")
                .difficulty(2)
                .likeCount(3)
                .owner(owner)
                .build();
    }

    // ─── save ─────────────────────────────────────────────────────

    @Test
    void saveEmptyFileThrowsIllegalArgumentTest() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.save(multipartFile, null, owner, "Song", "", 1));
    }

    @Test
    void saveWrongContentTypeThrowsIllegalArgumentTest() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");

        assertThrows(IllegalArgumentException.class,
                () -> service.save(multipartFile, null, owner, "Song", "", 1));
    }

    @Test
    void saveNullContentTypeThrowsIllegalArgumentTest() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.save(multipartFile, null, owner, "Song", "", 1));
    }

    @Test
    void saveValidPdfSavesDocumentTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getSize()).thenReturn((long) minimalPdf.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));
        when(repository.save(any(PdfDocument.class))).thenReturn(doc);

        PdfResponseDto result = service.save(multipartFile, null, owner, "Song Title", "Description", 2);

        assertNotNull(result);
        verify(repository).save(any(PdfDocument.class));
    }

    @Test
    void saveDifficultyOutOfRangeUsesDefaultTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getSize()).thenReturn((long) minimalPdf.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));
        when(repository.save(any(PdfDocument.class))).thenReturn(doc);

        service.save(multipartFile, null, owner, "Song", null, 10);

        verify(repository).save(argThat(d -> d.getDifficulty() == 1));
    }

    @Test
    void saveNullSongTitleSavesWithNullTitleTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getSize()).thenReturn((long) minimalPdf.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));
        when(repository.save(any(PdfDocument.class))).thenReturn(doc);

        PdfResponseDto result = service.save(multipartFile, null, owner, null, null, 1);

        assertNotNull(result);
    }

    @Test
    void saveNullOriginalFilenameUsesGeneratedNameTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(multipartFile.getSize()).thenReturn((long) minimalPdf.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));
        when(repository.save(any(PdfDocument.class))).thenReturn(doc);

        PdfResponseDto result = service.save(multipartFile, null, owner, "Song", "", 1);

        assertNotNull(result);
    }

    // ─── getPublicFeed ────────────────────────────────────────────

    @Test
    void getPublicFeedNoFiltersReturnsAllTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of(1L));

        List<PdfResponseDto> result = service.getPublicFeed(null, null, "NEWEST", owner);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).likedByMe());
    }

    @Test
    void getPublicFeedWithSearchUsesSearchRepoTest() {
        when(repository.findPublicFeedBySearch(eq("test"), any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed("test", null, "NEWEST", owner);

        assertNotNull(result);
        verify(repository).findPublicFeedBySearch(eq("test"), any());
    }

    @Test
    void getPublicFeedWithDifficultyUsesDifficultyRepoTest() {
        when(repository.findPublicFeedByDifficulty(eq(2), any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed(null, 2, "NEWEST", owner);

        assertNotNull(result);
        verify(repository).findPublicFeedByDifficulty(eq(2), any());
    }

    @Test
    void getPublicFeedWithSearchAndDifficultyUsesCombinedRepoTest() {
        when(repository.findPublicFeedBySearchAndDifficulty(eq("rock"), eq(2), any(Pageable.class)))
                .thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed("rock", 2, "NEWEST", owner);

        assertNotNull(result);
        verify(repository).findPublicFeedBySearchAndDifficulty(eq("rock"), eq(2), any());
    }

    @Test
    void getPublicFeedNullUserReturnsWithoutLikesTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of(doc));

        List<PdfResponseDto> result = service.getPublicFeed(null, null, "NEWEST", null);

        assertNotNull(result);
        assertFalse(result.get(0).likedByMe());
        verify(likeRepository, never()).findLikedPdfIds(any(), any());
    }

    @Test
    void getPublicFeedEmptyDocsSkipsLikeQueryTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed(null, null, "NEWEST", owner);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(likeRepository, never()).findLikedPdfIds(any(), any());
    }

    @Test
    void getPublicFeedBlankSearchTreatsAsNullTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        service.getPublicFeed("   ", null, "NEWEST", owner);

        verify(repository).findAllPublicFeed(any());
    }

    // ─── listByUser ───────────────────────────────────────────────

    @Test
    void listByUserReturnsUserDocsTest() {
        when(repository.findByOwnerOrderByUploadedAtDesc(owner)).thenReturn(List.of(doc));

        List<PdfResponseDto> result = service.listByUser(owner);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).likedByMe());
    }

    @Test
    void listByUserNoDocsReturnsEmptyTest() {
        when(repository.findByOwnerOrderByUploadedAtDesc(owner)).thenReturn(List.of());

        List<PdfResponseDto> result = service.listByUser(owner);

        assertTrue(result.isEmpty());
    }

    // ─── like ─────────────────────────────────────────────────────

    @Test
    void likeNotLikedBeforeSavesLikeAndIncrementsTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(likeRepository.existsByPdfIdAndUserId(1L, owner.getId())).thenReturn(false);

        PdfResponseDto result = service.like(1L, owner);

        assertNotNull(result);
        assertTrue(result.likedByMe());
        verify(likeRepository).save(any(PdfLike.class));
        verify(repository).incrementLikeCount(1L);
    }

    @Test
    void likeAlreadyLikedSkipsSaveTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(likeRepository.existsByPdfIdAndUserId(1L, owner.getId())).thenReturn(true);

        PdfResponseDto result = service.like(1L, owner);

        assertNotNull(result);
        assertTrue(result.likedByMe());
        verify(likeRepository, never()).save(any());
        verify(repository, never()).incrementLikeCount(any());
    }

    @Test
    void likeNotFoundThrowsExceptionTest() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.like(99L, owner));
    }

    // ─── unlike ───────────────────────────────────────────────────

    @Test
    void unlikeHasLikeDeletesAndDecrementsTest() {
        PdfLike like = PdfLike.builder().id(1L).pdf(doc).user(owner).build();
        doc.setLikeCount(3);

        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(likeRepository.findByPdfIdAndUserId(1L, owner.getId())).thenReturn(Optional.of(like));

        PdfResponseDto result = service.unlike(1L, owner);

        assertNotNull(result);
        assertFalse(result.likedByMe());
        verify(likeRepository).delete(like);
        verify(repository).decrementLikeCount(1L);
    }

    @Test
    void unlikeNoLikeDoesNothingTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(likeRepository.findByPdfIdAndUserId(1L, owner.getId())).thenReturn(Optional.empty());

        PdfResponseDto result = service.unlike(1L, owner);

        assertNotNull(result);
        verify(likeRepository, never()).delete(any());
    }

    @Test
    void unlikeLikeCountZeroDoesNotGoNegativeTest() {
        PdfLike like = PdfLike.builder().id(1L).pdf(doc).user(owner).build();
        doc.setLikeCount(0);

        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(likeRepository.findByPdfIdAndUserId(1L, owner.getId())).thenReturn(Optional.of(like));

        service.unlike(1L, owner);

        assertEquals(0, doc.getLikeCount());
    }

    // ─── findById ─────────────────────────────────────────────────

    @Test
    void findByIdExistingDocReturnsDocTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(doc));

        PdfDocument result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findByIdNotFoundThrowsRuntimeTest() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }

    // ─── delete ───────────────────────────────────────────────────

    @Test
    void deleteNotOwnerThrowsSecurityTest() {
        when(repository.findByIdAndOwner(1L, owner)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> service.delete(1L, owner));
    }

    // ─── PdfResponseDto ───────────────────────────────────────────

    @Test
    void pdfResponseDtoFromMapsFieldsCorrectlyTest() {
        PdfResponseDto dto = PdfResponseDto.from(doc, true);

        assertEquals(1L, dto.id());
        assertEquals("test.pdf", dto.originalName());
        assertEquals("stored.pdf", dto.fileName());
        assertEquals(1024L, dto.fileSize());
        assertEquals(5, dto.pageCount());
        assertEquals("Test Song", dto.songTitle());
        assertEquals(2, dto.difficulty());
        assertEquals(3, dto.likeCount());
        assertTrue(dto.likedByMe());
        assertEquals("Owner User", dto.ownerName());
        assertEquals(owner.getId(), dto.ownerId());
    }

    @Test
    void pdfResponseDtoFromWithoutLikeLikedByMeFalseTest() {
        PdfResponseDto dto = PdfResponseDto.from(doc);

        assertFalse(dto.likedByMe());
    }

    @Test
    void pdfResponseDtoNullOwnerReturnsNullOwnerFieldsTest() {
        doc.setOwner(null);
        PdfResponseDto dto = PdfResponseDto.from(doc, false);

        assertNull(dto.ownerName());
        assertNull(dto.ownerId());
    }

    @Test
    void pdfResponseDtoNonNullCoverImagePathPrefixedTest() {
        doc.setCoverImagePath("pdfs/covers/image.jpg");
        PdfResponseDto dto = PdfResponseDto.from(doc, false);

        assertEquals("/uploads/pdfs/covers/image.jpg", dto.coverImageUrl());
    }

    // ─── getPublicFeed sort variants ──────────────────────────────

    @Test
    void getPublicFeedOldestSortUsesAscendingTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed(null, null, "OLDEST", owner);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPublicFeedLeastLikedSortTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed(null, null, "LEAST_LIKED", owner);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPublicFeedMostLikedSortTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed(null, null, "MOST_LIKED", owner);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPublicFeedNullSortDefaultsToNewestTest() {
        when(repository.findAllPublicFeed(any(Pageable.class))).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicFeed(null, null, null, owner);

        assertNotNull(result);
    }

    // ─── getByIdForUser ───────────────────────────────────────────

    @Test
    void getByIdForUserCurrentUserLikedReturnsLikedByMeTrueTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(doc));
        when(likeRepository.existsByPdfIdAndUserId(1L, owner.getId())).thenReturn(true);

        PdfResponseDto result = service.getByIdForUser(1L, owner);

        assertNotNull(result);
        assertTrue(result.likedByMe());
    }

    @Test
    void getByIdForUserNullUserReturnsFalseForLikedByMeTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(doc));

        PdfResponseDto result = service.getByIdForUser(1L, null);

        assertNotNull(result);
        assertFalse(result.likedByMe());
    }

    // ─── getPublicTabsByUser ───────────────────────────────────────

    @Test
    void getPublicTabsByUserWithViewerReturnsLikedByMeCorrectlyTest() {
        when(repository.findPublicByOwnerIdOrderByUploadedAtDesc(owner.getId())).thenReturn(List.of(doc));
        when(likeRepository.findLikedPdfIds(eq(owner.getId()), any())).thenReturn(List.of(1L));

        List<PdfResponseDto> result = service.getPublicTabsByUser(owner.getId(), owner);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).likedByMe());
    }

    @Test
    void getPublicTabsByUserNullViewerReturnsFalseForLikedByMeTest() {
        when(repository.findPublicByOwnerIdOrderByUploadedAtDesc(owner.getId())).thenReturn(List.of(doc));

        List<PdfResponseDto> result = service.getPublicTabsByUser(owner.getId(), null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).likedByMe());
    }

    @Test
    void getPublicTabsByUserEmptyDocsReturnsEmptyTest() {
        when(repository.findPublicByOwnerIdOrderByUploadedAtDesc(owner.getId())).thenReturn(List.of());

        List<PdfResponseDto> result = service.getPublicTabsByUser(owner.getId(), owner);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ─── loadAsResource ───────────────────────────────────────────

    @Test
    void loadAsResourceFileNotFoundThrowsRuntimeExceptionTest() {
        doc.setFilePath("/nonexistent/path/to/file.pdf");
        when(repository.findById(1L)).thenReturn(Optional.of(doc));

        assertThrows(RuntimeException.class, () -> service.loadAsResource(1L));
    }

    // ─── delete success ───────────────────────────────────────────

    @Test
    void deleteSuccessDeletesFileAndRecordTest() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("wavii-test", ".pdf");
        tempFile.deleteOnExit();
        doc.setFilePath(tempFile.getAbsolutePath());
        doc.setCoverImagePath(null);

        when(repository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(doc));

        service.delete(1L, owner);

        verify(likeRepository).deleteByPdfId(1L);
        verify(reportRepository).deleteByPdfDocumentId(1L);
        verify(repository).delete(doc);
    }

    @Test
    void deleteWithCoverImageDeletesCoverTest() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("wavii-test", ".pdf");
        tempFile.deleteOnExit();
        doc.setFilePath(tempFile.getAbsolutePath());
        doc.setCoverImagePath("pdfs/covers/image.jpg");

        when(repository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(doc));

        service.delete(1L, owner);

        verify(repository).delete(doc);
    }

    // ─── save with cover image ────────────────────────────────────

    @Test
    void saveWithCoverImageSavesBothFilesTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        MultipartFile coverFile = mock(MultipartFile.class);

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getSize()).thenReturn((long) minimalPdf.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));

        when(coverFile.isEmpty()).thenReturn(false);
        when(coverFile.getContentType()).thenReturn("image/jpeg");
        when(coverFile.getOriginalFilename()).thenReturn("cover.jpg");
        when(coverFile.getInputStream()).thenReturn(new ByteArrayInputStream("fake-image".getBytes()));

        when(repository.save(any(PdfDocument.class))).thenReturn(doc);

        PdfResponseDto result = service.save(multipartFile, coverFile, owner, "Song", "Desc", 2);

        assertNotNull(result);
        verify(repository).save(argThat(d -> d.getCoverImagePath() != null));
    }

    @Test
    void saveWithCoverImageInvalidTypeThrowsTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        MultipartFile coverFile = mock(MultipartFile.class);

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));

        when(coverFile.isEmpty()).thenReturn(false);
        when(coverFile.getContentType()).thenReturn("application/pdf");

        assertThrows(IllegalArgumentException.class,
                () -> service.save(multipartFile, coverFile, owner, "Song", "Desc", 2));
    }

    @Test
    void saveWithNullCoverImageContentTypeThrowsTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        MultipartFile coverFile = mock(MultipartFile.class);

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));

        when(coverFile.isEmpty()).thenReturn(false);
        when(coverFile.getContentType()).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.save(multipartFile, coverFile, owner, "Song", "Desc", 2));
    }

    @Test
    void saveWithEmptyCoverImageSkipsCoverTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        MultipartFile coverFile = mock(MultipartFile.class);

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getSize()).thenReturn((long) minimalPdf.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));

        when(coverFile.isEmpty()).thenReturn(true);

        when(repository.save(any(PdfDocument.class))).thenReturn(doc);

        PdfResponseDto result = service.save(multipartFile, coverFile, owner, "Song", "Desc", 2);

        assertNotNull(result);
        verify(repository).save(argThat(d -> d.getCoverImagePath() == null));
    }

    @Test
    void saveWithCoverImageNoExtensionUsesJpgFallbackTest() throws Exception {
        byte[] minimalPdf = "%PDF-1.4\n%%EOF\n".getBytes();
        MultipartFile coverFile = mock(MultipartFile.class);

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getSize()).thenReturn((long) minimalPdf.length);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(minimalPdf));

        when(coverFile.isEmpty()).thenReturn(false);
        when(coverFile.getContentType()).thenReturn("image/jpeg");
        when(coverFile.getOriginalFilename()).thenReturn(null);
        when(coverFile.getInputStream()).thenReturn(new ByteArrayInputStream("fake-image".getBytes()));

        when(repository.save(any(PdfDocument.class))).thenReturn(doc);

        PdfResponseDto result = service.save(multipartFile, coverFile, owner, "Song", "Desc", 2);

        assertNotNull(result);
        verify(repository).save(argThat(d -> d.getCoverImagePath() != null && d.getCoverImagePath().endsWith(".jpg")));
    }

    // ─── loadAsResource when file exists ─────────────────────────

    @Test
    void loadAsResourceExistingFileReturnsResourceTest() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("wavii-load-test", ".pdf");
        tempFile.deleteOnExit();
        doc.setFilePath(tempFile.getAbsolutePath());

        when(repository.findById(1L)).thenReturn(Optional.of(doc));

        org.springframework.core.io.Resource resource = service.loadAsResource(1L);

        assertNotNull(resource);
        assertTrue(resource.exists());
    }
}
