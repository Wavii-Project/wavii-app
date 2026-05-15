package com.wavii.controller;

import com.wavii.dto.forum.CreateForumRequest;
import com.wavii.dto.forum.CreatePostRequest;
import com.wavii.dto.forum.ForumMemberResponse;
import com.wavii.dto.forum.ForumResponse;
import com.wavii.dto.forum.ForumSummaryResponse;
import com.wavii.dto.forum.PostResponse;
import com.wavii.dto.forum.UpdateForumMemberRoleRequest;
import com.wavii.model.User;
import com.wavii.model.enums.ForumCategory;
import com.wavii.model.enums.ForumMembershipRole;
import com.wavii.service.ForumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumControllerTest {

    @Mock private ForumService forumService;

    @InjectMocks private ForumController controller;

    private User user;
    private UUID forumId;
    private ForumSummaryResponse summaryResponse;
    private ForumResponse forumResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        forumId = UUID.randomUUID();
        summaryResponse = new ForumSummaryResponse(
                forumId.toString(), "Test Forum", "Desc", "GENERAL",
                5, false, "http://cover.url", "Creator", "Madrid", 0, false);
        forumResponse = new ForumResponse(
                forumId.toString(), "Test Forum", "Desc", "GENERAL",
                5, true, "http://cover.url", null, "Creator", "Madrid",
                "2024-01-01T00:00:00", 0, false, null, java.util.List.of());
    }

    // ─── getForums ────────────────────────────────────────────────

    @Test
    void getForumsNoFiltersReturnsOkTest() {
        when(forumService.getForums(null, null, null, null, user)).thenReturn(List.of(summaryResponse));

        ResponseEntity<List<ForumSummaryResponse>> result =
                controller.getForums(null, null, null, null, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getForumsWithFiltersReturnsOkTest() {
        when(forumService.getForums("jazz", "Madrid", null, null, user)).thenReturn(List.of(summaryResponse));

        ResponseEntity<List<ForumSummaryResponse>> result =
                controller.getForums("jazz", "Madrid", null, null, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getForumsEmptyReturnsOkTest() {
        when(forumService.getForums(null, null, null, null, user)).thenReturn(List.of());

        ResponseEntity<List<ForumSummaryResponse>> result =
                controller.getForums(null, null, null, null, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    // ─── getMyForums ──────────────────────────────────────────────

    @Test
    void getMyForumsReturnsOkTest() {
        when(forumService.getMyForums(user)).thenReturn(List.of(summaryResponse));

        ResponseEntity<List<ForumSummaryResponse>> result = controller.getMyForums(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    // ─── getForum ─────────────────────────────────────────────────

    @Test
    void getForumExistingIdReturnsOkTest() {
        when(forumService.getForumById(forumId, user)).thenReturn(forumResponse);

        ResponseEntity<ForumResponse> result = controller.getForum(forumId, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(forumResponse, result.getBody());
    }

    @Test
    void getForumNotFoundPropagatesExceptionTest() {
        when(forumService.getForumById(forumId, user))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> controller.getForum(forumId, user));
    }

    // ─── createForum ─────────────────────────────────────────────

    @Test
    void createForumValidReturns201Test() {
        CreateForumRequest req = new CreateForumRequest(
                "New Forum", "Desc", ForumCategory.GENERAL, null, "Madrid");
        when(forumService.createForum(req, user)).thenReturn(forumResponse);

        ResponseEntity<ForumResponse> result = controller.createForum(req, user);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(forumResponse, result.getBody());
    }

    @Test
    void createForumInvalidPropagatesExceptionTest() {
        CreateForumRequest req = new CreateForumRequest(null, null, null, null, null);
        when(forumService.createForum(req, user))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThrows(ResponseStatusException.class, () -> controller.createForum(req, user));
    }

    // ─── joinForum ────────────────────────────────────────────────

    @Test
    void joinForumSuccessReturnsOkTest() {
        doNothing().when(forumService).joinForum(forumId, user);

        ResponseEntity<Void> result = controller.joinForum(forumId, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(forumService).joinForum(forumId, user);
    }

    @Test
    void joinForumAlreadyMemberPropagatesConflictTest() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT))
                .when(forumService).joinForum(forumId, user);

        assertThrows(ResponseStatusException.class, () -> controller.joinForum(forumId, user));
    }

    // ─── leaveForum ──────────────────────────────────────────────

    @Test
    void leaveForumSuccessReturnsOkTest() {
        doNothing().when(forumService).leaveForum(forumId, user);

        ResponseEntity<Void> result = controller.leaveForum(forumId, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(forumService).leaveForum(forumId, user);
    }

    @Test
    void leaveForumNotMemberPropagatesExceptionTest() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND))
                .when(forumService).leaveForum(forumId, user);

        assertThrows(ResponseStatusException.class, () -> controller.leaveForum(forumId, user));
    }

    // ─── getPosts ────────────────────────────────────────────────

    @Test
    void getPostsMemberReturnsOkTest() {
        PostResponse postResponse = new PostResponse(
                UUID.randomUUID().toString(), "Content",
                user.getId().toString(), "User", null, "2024-01-01T00:00:00");
        Page<PostResponse> page = new PageImpl<>(List.of(postResponse));
        when(forumService.getPosts(forumId, 0, user)).thenReturn(page);

        ResponseEntity<Page<PostResponse>> result = controller.getPosts(forumId, 0, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    void getPostsNotMemberPropagatesForbiddenTest() {
        when(forumService.getPosts(forumId, 0, user))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> controller.getPosts(forumId, 0, user));
    }

    // ─── createPost ──────────────────────────────────────────────

    @Test
    void createPostValidReturns201Test() {
        CreatePostRequest req = new CreatePostRequest("Hello world");
        PostResponse postResponse = new PostResponse(
                UUID.randomUUID().toString(), "Hello world",
                user.getId().toString(), "User", null, "2024-01-01T00:00:00");
        when(forumService.createPost(forumId, req, user)).thenReturn(postResponse);

        ResponseEntity<PostResponse> result = controller.createPost(forumId, req, user);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(postResponse, result.getBody());
    }

    @Test
    void createPostBlankContentPropagatesBadRequestTest() {
        CreatePostRequest req = new CreatePostRequest("  ");
        when(forumService.createPost(forumId, req, user))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThrows(ResponseStatusException.class, () -> controller.createPost(forumId, req, user));
    }

    @Test
    void createPostNotMemberPropagatesForbiddenTest() {
        CreatePostRequest req = new CreatePostRequest("Hello");
        when(forumService.createPost(forumId, req, user))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> controller.createPost(forumId, req, user));
    }

    // ─── likeForum ────────────────────────────────────────────────

    @Test
    void likeForumReturnsOkTest() {
        when(forumService.likeForum(forumId, user)).thenReturn(forumResponse);

        ResponseEntity<ForumResponse> result = controller.likeForum(forumId, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(forumResponse, result.getBody());
    }

    // ─── unlikeForum ─────────────────────────────────────────────

    @Test
    void unlikeForumReturnsOkTest() {
        when(forumService.unlikeForum(forumId, user)).thenReturn(forumResponse);

        ResponseEntity<ForumResponse> result = controller.unlikeForum(forumId, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(forumResponse, result.getBody());
    }

    // ─── getMembers ───────────────────────────────────────────────

    @Test
    void getMembersReturnsOkTest() {
        ForumMemberResponse member = new ForumMemberResponse(
                user.getId().toString(), "Test User", null, "MEMBER", "2024-01-01T00:00:00");
        when(forumService.getMembers(forumId, user)).thenReturn(List.of(member));

        ResponseEntity<?> result = controller.getMembers(forumId, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ─── updateMemberRole ─────────────────────────────────────────

    @Test
    void updateMemberRoleReturnsOkTest() {
        UUID memberId = UUID.randomUUID();
        UpdateForumMemberRoleRequest req = new UpdateForumMemberRoleRequest(ForumMembershipRole.ADMIN);
        when(forumService.updateMemberRole(forumId, memberId, ForumMembershipRole.ADMIN, user))
                .thenReturn(forumResponse);

        ResponseEntity<ForumResponse> result = controller.updateMemberRole(forumId, memberId, req, user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(forumService).updateMemberRole(forumId, memberId, ForumMembershipRole.ADMIN, user);
    }

    // ─── removeMember ─────────────────────────────────────────────

    @Test
    void removeMemberReturns204Test() {
        UUID memberId = UUID.randomUUID();
        doNothing().when(forumService).removeMember(forumId, memberId, user);

        ResponseEntity<Void> result = controller.removeMember(forumId, memberId, user);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(forumService).removeMember(forumId, memberId, user);
    }

    // ─── uploadForumImage ─────────────────────────────────────────

    @Test
    void uploadForumImageNullFileReturnsBadRequestTest() {
        ReflectionTestUtils.setField(controller, "pdfStoragePath", "./uploads/pdfs");
        ReflectionTestUtils.setField(controller, "appBaseUrl", "http://localhost:8080");

        ResponseEntity<?> result = controller.uploadForumImage(null);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void uploadForumImageEmptyFileReturnsBadRequestTest() {
        ReflectionTestUtils.setField(controller, "pdfStoragePath", "./uploads/pdfs");
        ReflectionTestUtils.setField(controller, "appBaseUrl", "http://localhost:8080");
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        ResponseEntity<?> result = controller.uploadForumImage(file);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void uploadForumImageNonImageContentTypeReturnsBadRequestTest() {
        ReflectionTestUtils.setField(controller, "pdfStoragePath", "./uploads/pdfs");
        ReflectionTestUtils.setField(controller, "appBaseUrl", "http://localhost:8080");
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        ResponseEntity<?> result = controller.uploadForumImage(file);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void uploadForumImageNullContentTypeReturnsBadRequestTest() {
        ReflectionTestUtils.setField(controller, "pdfStoragePath", "./uploads/pdfs");
        ReflectionTestUtils.setField(controller, "appBaseUrl", "http://localhost:8080");
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", null, "content".getBytes());

        ResponseEntity<?> result = controller.uploadForumImage(file);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void uploadForumImageValidImageReturnsOkTest() {
        ReflectionTestUtils.setField(controller, "pdfStoragePath",
                System.getProperty("java.io.tmpdir") + "/pdfs");
        ReflectionTestUtils.setField(controller, "appBaseUrl", "http://localhost:8080");
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "image-data".getBytes());

        ResponseEntity<?> result = controller.uploadForumImage(file);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void uploadForumImageNoExtensionUsesDefaultJpgTest() {
        ReflectionTestUtils.setField(controller, "pdfStoragePath",
                System.getProperty("java.io.tmpdir") + "/pdfs");
        ReflectionTestUtils.setField(controller, "appBaseUrl", "http://localhost:8080");
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover", "image/jpeg", "image-data".getBytes());

        ResponseEntity<?> result = controller.uploadForumImage(file);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
