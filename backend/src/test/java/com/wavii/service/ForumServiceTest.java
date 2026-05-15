package com.wavii.service;

import com.wavii.dto.forum.CreateForumRequest;
import com.wavii.dto.forum.CreatePostRequest;
import com.wavii.dto.forum.ForumResponse;
import com.wavii.dto.forum.ForumSummaryResponse;
import com.wavii.dto.forum.PostResponse;
import com.wavii.model.Forum;
import com.wavii.model.ForumMembership;
import com.wavii.model.ForumPost;
import com.wavii.model.User;
import com.wavii.model.enums.ForumCategory;
import com.wavii.repository.ForumLikeRepository;
import com.wavii.repository.ForumMembershipRepository;
import com.wavii.repository.ForumPostRepository;
import com.wavii.repository.ForumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumServiceTest {

    @Mock private ForumRepository forumRepository;
    @Mock private ForumMembershipRepository membershipRepository;
    @Mock private ForumPostRepository postRepository;
    @Mock private ForumLikeRepository likeRepository;

    @InjectMocks private ForumService service;

    private User user;
    private Forum forum;
    private UUID forumId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setAvatarUrl("http://avatar.url");

        forumId = UUID.randomUUID();
        forum = Forum.builder()
                .id(forumId)
                .name("Test Forum")
                .description("Forum description")
                .category(ForumCategory.GENERAL)
                .coverImageUrl("http://cover.url")
                .city("Madrid")
                .creator(user)
                .memberCount(1)
                .build();
        forum.setCreatedAt(LocalDateTime.now());
    }

    // ─── getForums ────────────────────────────────────────────────

    @Test
    void getForumsNoFiltersReturnsAllTest() {
        when(forumRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(forum)));
        when(membershipRepository.findJoinedForumIds(eq(user), any())).thenReturn(List.of(forumId));
        when(likeRepository.findLikedForumIds(eq(user), any())).thenReturn(List.of());

        List<ForumSummaryResponse> result = service.getForums(null, null, null, null, user);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).joined());
    }

    @Test
    void getForumsWithSearchPassesSearchToRepoTest() {
        when(forumRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(forum)));
        when(membershipRepository.findJoinedForumIds(eq(user), any())).thenReturn(List.of());
        when(likeRepository.findLikedForumIds(eq(user), any())).thenReturn(List.of());

        List<ForumSummaryResponse> result = service.getForums("jazz", null, null, null, user);

        assertNotNull(result);
        assertFalse(result.get(0).joined());
    }

    @Test
    void getForumsWithCityPassesCityToRepoTest() {
        when(forumRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(forum)));
        when(membershipRepository.findJoinedForumIds(eq(user), any())).thenReturn(List.of());
        when(likeRepository.findLikedForumIds(eq(user), any())).thenReturn(List.of());

        List<ForumSummaryResponse> result = service.getForums(null, "Madrid", null, null, user);

        assertNotNull(result);
    }

    @Test
    void getForumsBlankSearchTreatsAsNullTest() {
        when(forumRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        List<ForumSummaryResponse> result = service.getForums("   ", "   ", null, null, user);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getForumsNullUserReturnsForumsNotJoinedTest() {
        when(forumRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(forum)));

        List<ForumSummaryResponse> result = service.getForums(null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).joined());
        verify(membershipRepository, never()).findJoinedForumIds(any(), any());
    }

    @Test
    void getForumsEmptyResultDoesNotQueryMembershipTest() {
        when(forumRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        List<ForumSummaryResponse> result = service.getForums(null, null, null, null, user);

        assertTrue(result.isEmpty());
        verify(membershipRepository, never()).findJoinedForumIds(any(), any());
    }

    // ─── getForumById ─────────────────────────────────────────────

    @Test
    void getForumByIdExistingForumReturnsResponseTest() {
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);
        when(likeRepository.existsByForumAndUser(forum, user)).thenReturn(false);

        ForumResponse result = service.getForumById(forumId, user);

        assertNotNull(result);
        assertEquals(forumId.toString(), result.id());
        assertEquals("Test Forum", result.name());
        assertTrue(result.joined());
    }

    @Test
    void getForumByIdNotFoundThrowsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(forumRepository.findById(unknownId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getForumById(unknownId, user));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getForumByIdNullUserReturnsNotJoinedTest() {
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));

        ForumResponse result = service.getForumById(forumId, null);

        assertFalse(result.joined());
        assertFalse(result.likedByMe());
    }

    // ─── createForum ─────────────────────────────────────────────

    @Test
    void createForumValidSavesForumAndMembershipTest() {
        CreateForumRequest req = new CreateForumRequest(
                "New Forum", "Description", ForumCategory.GENERAL, "http://img.url", "Madrid");

        when(forumRepository.save(any(Forum.class))).thenReturn(forum);
        when(membershipRepository.save(any(ForumMembership.class))).thenReturn(null);

        ForumResponse result = service.createForum(req, user);

        assertNotNull(result);
        assertTrue(result.joined());
        verify(forumRepository).save(any(Forum.class));
        verify(membershipRepository).save(any(ForumMembership.class));
    }

    @Test
    void createForumBlankNameThrowsBadRequestTest() {
        CreateForumRequest req = new CreateForumRequest(
                "  ", "Description", ForumCategory.GENERAL, null, null);

        assertThrows(ResponseStatusException.class, () -> service.createForum(req, user));
    }

    @Test
    void createForumNullNameThrowsBadRequestTest() {
        CreateForumRequest req = new CreateForumRequest(
                null, "Description", ForumCategory.GENERAL, null, null);

        assertThrows(ResponseStatusException.class, () -> service.createForum(req, user));
    }

    @Test
    void createForumNullCategoryThrowsBadRequestTest() {
        CreateForumRequest req = new CreateForumRequest(
                "Forum", "Description", null, null, null);

        assertThrows(ResponseStatusException.class, () -> service.createForum(req, user));
    }

    @Test
    void createForumNullCitySavesWithNullCityTest() {
        CreateForumRequest req = new CreateForumRequest(
                "Forum", "Desc", ForumCategory.GENERAL, null, null);

        when(forumRepository.save(any(Forum.class))).thenReturn(forum);

        ForumResponse result = service.createForum(req, user);

        assertNotNull(result);
    }

    @Test
    void createForumBlankCitySavesWithNullCityTest() {
        CreateForumRequest req = new CreateForumRequest(
                "Forum", "Desc", ForumCategory.GENERAL, null, "  ");

        when(forumRepository.save(any(Forum.class))).thenReturn(forum);

        ForumResponse result = service.createForum(req, user);

        assertNotNull(result);
    }

    // ─── joinForum ────────────────────────────────────────────────

    @Test
    void joinForumNotMemberSavesMembershipTest() {
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(false);

        service.joinForum(forumId, user);

        verify(membershipRepository).save(any(ForumMembership.class));
        verify(forumRepository).incrementMemberCount(forumId);
    }

    @Test
    void joinForumAlreadyMemberThrowsConflictTest() {
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.joinForum(forumId, user));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void joinForumForumNotFoundThrowsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(forumRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.joinForum(unknownId, user));
    }

    // ─── leaveForum ──────────────────────────────────────────────

    @Test
    void leaveForumIsMemberDeletesMembershipTest() {
        ForumMembership membership = ForumMembership.builder()
                .forum(forum).user(user).build();

        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.findByForumAndUser(forum, user)).thenReturn(Optional.of(membership));
        when(membershipRepository.findByForumOrderByJoinedAtAsc(forum)).thenReturn(List.of());

        service.leaveForum(forumId, user);

        verify(membershipRepository).delete(membership);
        verify(forumRepository).delete(forum);
    }

    @Test
    void leaveForumNotMemberThrowsNotFoundTest() {
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.findByForumAndUser(forum, user)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.leaveForum(forumId, user));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void leaveForumForumNotFoundThrowsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(forumRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.leaveForum(unknownId, user));
    }

    // ─── getMyForums ──────────────────────────────────────────────

    @Test
    void getMyForumsReturnsMembershipsTest() {
        ForumMembership membership = ForumMembership.builder()
                .forum(forum).user(user).build();
        when(membershipRepository.findByUserWithForum(user)).thenReturn(List.of(membership));
        when(likeRepository.existsByForumAndUser(any(), any())).thenReturn(false);

        List<ForumSummaryResponse> result = service.getMyForums(user);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).joined());
        assertEquals("Test Forum", result.get(0).name());
    }

    @Test
    void getMyForumsNoMembershipsReturnsEmptyTest() {
        when(membershipRepository.findByUserWithForum(user)).thenReturn(List.of());

        List<ForumSummaryResponse> result = service.getMyForums(user);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ─── getPosts ────────────────────────────────────────────────

    @Test
    void getPostsMemberReturnsPostsTest() {
        ForumPost post = buildPost();
        Page<ForumPost> postPage = new PageImpl<>(List.of(post));

        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);
        when(postRepository.findByForumOrderByCreatedAtDesc(eq(forum), any())).thenReturn(postPage);

        Page<PostResponse> result = service.getPosts(forumId, 0, user);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPostsNotMemberThrowsForbiddenTest() {
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getPosts(forumId, 0, user));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getPostsForumNotFoundThrowsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(forumRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.getPosts(unknownId, 0, user));
    }

    // ─── createPost ──────────────────────────────────────────────

    @Test
    void createPostMemberSavesAndReturnsPostTest() {
        CreatePostRequest req = new CreatePostRequest("Hello world");
        ForumPost post = buildPost();

        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);
        when(postRepository.save(any(ForumPost.class))).thenReturn(post);

        PostResponse result = service.createPost(forumId, req, user);

        assertNotNull(result);
        verify(postRepository).save(any(ForumPost.class));
    }

    @Test
    void createPostNotMemberThrowsForbiddenTest() {
        CreatePostRequest req = new CreatePostRequest("Hello");
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createPost(forumId, req, user));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void createPostBlankContentThrowsBadRequestTest() {
        CreatePostRequest req = new CreatePostRequest("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createPost(forumId, req, user));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createPostNullContentThrowsBadRequestTest() {
        CreatePostRequest req = new CreatePostRequest(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createPost(forumId, req, user));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createPostForumNotFoundThrowsNotFoundTest() {
        CreatePostRequest req = new CreatePostRequest("Hello");
        UUID unknownId = UUID.randomUUID();
        when(forumRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.createPost(unknownId, req, user));
    }

    // ─── helpers ─────────────────────────────────────────────────

    private ForumPost buildPost() {
        ForumPost post = ForumPost.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .author(user)
                .content("Test content")
                .build();
        post.setCreatedAt(LocalDateTime.now());
        return post;
    }
}
