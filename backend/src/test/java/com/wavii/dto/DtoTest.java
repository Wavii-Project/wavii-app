package com.wavii.dto;

import com.wavii.dto.auth.AuthResponse;
import com.wavii.dto.auth.ForgotPasswordRequest;
import com.wavii.dto.auth.LoginRequest;
import com.wavii.dto.auth.RefreshTokenRequest;
import com.wavii.dto.auth.RegisterRequest;
import com.wavii.dto.auth.ResetPasswordRequest;
import com.wavii.dto.bulletin.BulletinTeacherResponse;
import com.wavii.dto.bulletin.BulletinUpdateRequest;
import com.wavii.dto.challenge.DailyChallengeDto;
import com.wavii.dto.forum.ForumMemberResponse;
import com.wavii.dto.forum.UpdateForumMemberRoleRequest;
import com.wavii.dto.onboarding.CompleteOnboardingRequest;
import com.wavii.dto.onboarding.VerificationStatusResponse;
import com.wavii.dto.user.PublicUserProfileDto;
import com.wavii.model.DailyChallenge;
import com.wavii.model.PdfDocument;
import com.wavii.model.User;
import com.wavii.model.enums.ForumMembershipRole;
import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void authResponseBuilderSetsAllFieldsTest() {
        UUID id = UUID.randomUUID();
        AuthResponse response = AuthResponse.builder()
                .accessToken("access_token_value")
                .refreshToken("refresh_token_value")
                .userId(id)
                .name("Test User")
                .email("test@test.com")
                .city("Madrid")
                .role(Role.USUARIO)
                .subscription(Subscription.FREE)
                .emailVerified(true)
                .onboardingCompleted(false)
                .teacherVerified(false)
                .build();

        assertEquals("access_token_value", response.getAccessToken());
        assertEquals("refresh_token_value", response.getRefreshToken());
        assertEquals(id, response.getUserId());
        assertEquals("Test User", response.getName());
        assertEquals("test@test.com", response.getEmail());
        assertEquals("Madrid", response.getCity());
        assertEquals(Role.USUARIO, response.getRole());
        assertEquals(Subscription.FREE, response.getSubscription());
        assertTrue(response.isEmailVerified());
        assertFalse(response.isOnboardingCompleted());
        assertFalse(response.isTeacherVerified());
    }

    @Test
    void loginRequestSettersGettersTest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("login@test.com");
        request.setPassword("password123");
        assertEquals("login@test.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void registerRequestSettersGettersTest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@test.com");
        request.setPassword("securePass123");
        assertEquals("New User", request.getName());
        assertEquals("new@test.com", request.getEmail());
        assertEquals("securePass123", request.getPassword());
    }

    @Test
    void forgotPasswordRequestSettersGettersTest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("forgot@test.com");
        assertEquals("forgot@test.com", request.getEmail());
    }

    @Test
    void resetPasswordRequestSettersGettersTest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("newPassword456");
        assertEquals("reset-token-123", request.getToken());
        assertEquals("newPassword456", request.getNewPassword());
    }

    @Test
    void refreshTokenRequestSettersGettersTest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh_token_value");
        assertEquals("refresh_token_value", request.getRefreshToken());
    }

    @Test
    void bulletinTeacherResponseRecordAccessorsTest() {
        BulletinTeacherResponse response = new BulletinTeacherResponse(
                "uuid-123", "Prof. Ana", "profesor_certificado",
                "Clases de piano", "Piano", new BigDecimal("30.00"), "Madrid",
                40.4168, -3.7038, "Calle Mayor 1", "Madrid", "ana@wavii.app", "600000000",
                "https://instagram.com/profana", null, null, null,
                null, List.<String>of(), "ANYTIME", "Tardes entre semana", "PRESENCIAL");

        assertEquals("uuid-123", response.id());
        assertEquals("Prof. Ana", response.name());
        assertEquals("profesor_certificado", response.role());
        assertEquals("Clases de piano", response.bio());
        assertEquals("Piano", response.instrument());
        assertEquals(new BigDecimal("30.00"), response.pricePerHour());
        assertEquals("Madrid", response.city());
        assertEquals("PRESENCIAL", response.classModality());
        assertEquals("ANYTIME", response.availabilityPreference());
    }

    @Test
    void bulletinTeacherResponseNullFieldsTest() {
        BulletinTeacherResponse response = new BulletinTeacherResponse(
                "uuid-456", "Prof. Juan", "profesor_particular",
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                null, List.<String>of(), null, null, null);

        assertNull(response.bio());
        assertNull(response.instrument());
        assertNull(response.pricePerHour());
        assertNull(response.city());
        assertNull(response.availabilityNotes());
    }

    @Test
    void bulletinUpdateRequestRecordAccessorsTest() {
        BulletinUpdateRequest request = new BulletinUpdateRequest(
                "Guitarra", new BigDecimal("25.00"), "Profesor de guitarra clasica",
                "Sevilla", 37.3861, -5.9925, "Plaza Espana 1", "Sevilla", "profesor@wavii.app",
                "600000000", "https://instagram.com/profesor", null, null, null,
                null, List.<String>of(), "AFTERNOON", "Martes y jueves", "PRESENCIAL");

        assertEquals("Guitarra", request.instrument());
        assertEquals(new BigDecimal("25.00"), request.pricePerHour());
        assertEquals("Profesor de guitarra clasica", request.bio());
        assertEquals("Sevilla", request.city());
        assertEquals("AFTERNOON", request.availabilityPreference());
    }

    @Test
    void completeOnboardingRequestSettersGettersTest() {
        CompleteOnboardingRequest request = new CompleteOnboardingRequest();
        request.setRole(Role.PROFESOR_PARTICULAR);
        request.setLevel(Level.AVANZADO);
        request.setAvatarUrl("http://example.com/avatar.png");

        assertEquals(Role.PROFESOR_PARTICULAR, request.getRole());
        assertEquals(Level.AVANZADO, request.getLevel());
        assertEquals("http://example.com/avatar.png", request.getAvatarUrl());
    }

    @Test
    void verificationStatusResponseBuilderSetsFieldsTest() {
        VerificationStatusResponse response = VerificationStatusResponse.builder()
                .teacherVerified(true)
                .message("Verificado correctamente")
                .build();

        assertTrue(response.isTeacherVerified());
        assertEquals("Verificado correctamente", response.getMessage());
    }

    // ─── ForumMemberResponse ──────────────────────────────────────

    @Test
    void forumMemberResponseRecordAccessorsTest() {
        ForumMemberResponse response = new ForumMemberResponse(
                "user-id-123", "Ana Garcia", "http://avatar.png", "MEMBER", "2024-01-01T00:00:00");
        assertEquals("user-id-123", response.userId());
        assertEquals("Ana Garcia", response.name());
        assertEquals("http://avatar.png", response.avatarUrl());
        assertEquals("MEMBER", response.role());
        assertEquals("2024-01-01T00:00:00", response.joinedAt());
    }

    @Test
    void forumMemberResponseNullFieldsTest() {
        ForumMemberResponse response = new ForumMemberResponse("id", "Name", null, "MEMBER", null);
        assertNull(response.avatarUrl());
        assertNull(response.joinedAt());
    }

    // ─── UpdateForumMemberRoleRequest ─────────────────────────────

    @Test
    void updateForumMemberRoleRequestRecordAccessorTest() {
        UpdateForumMemberRoleRequest request = new UpdateForumMemberRoleRequest(ForumMembershipRole.ADMIN);
        assertEquals(ForumMembershipRole.ADMIN, request.role());
    }

    // ─── PublicUserProfileDto ─────────────────────────────────────

    @Test
    void publicUserProfileDtoFromWithAllFieldsTest() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Ana")
                .level(Level.AVANZADO)
                .role(Role.PROFESOR_PARTICULAR)
                .xp(500)
                .streak(10)
                .createdAt(LocalDateTime.of(2024, 5, 15, 10, 0))
                .build();

        PublicUserProfileDto dto = PublicUserProfileDto.from(user, 3);

        assertEquals(user.getId(), dto.id());
        assertEquals("Ana", dto.name());
        assertEquals("AVANZADO", dto.level());
        assertEquals("PROFESOR_PARTICULAR", dto.role());
        assertEquals(500, dto.xp());
        assertEquals(10, dto.streak());
        assertEquals(3, dto.tabsPublished());
        assertEquals("2024-05", dto.memberSince());
    }

    @Test
    void publicUserProfileDtoFromWithNullLevelAndRoleTest() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Juan")
                .level(null)
                .role(null)
                .xp(0)
                .streak(0)
                .createdAt(null)
                .build();

        PublicUserProfileDto dto = PublicUserProfileDto.from(user, 0);

        assertNull(dto.level());
        assertNull(dto.role());
        assertNull(dto.memberSince());
    }

    // ─── DailyChallengeDto ────────────────────────────────────────

    @Test
    void dailyChallengeDtoFromWithOwnerAndCoverTest() {
        User owner = User.builder().name("Prof. Ana").build();
        PdfDocument pdf = PdfDocument.builder()
                .id(42L)
                .songTitle("Clair de Lune")
                .description("Classic piece")
                .coverImagePath("covers/cover.png")
                .owner(owner)
                .build();
        DailyChallenge challenge = DailyChallenge.builder()
                .id(1L)
                .challengeDate(LocalDate.of(2024, 1, 1))
                .difficulty(Level.INTERMEDIO)
                .xpReward(25)
                .pdfDocument(pdf)
                .build();

        DailyChallengeDto dto = DailyChallengeDto.from(challenge, true);

        assertEquals(1L, dto.id());
        assertEquals(Level.INTERMEDIO, dto.difficulty());
        assertEquals(25, dto.xpReward());
        assertEquals("Clair de Lune", dto.tabTitle());
        assertEquals("Prof. Ana", dto.tabOwnerName());
        assertTrue(dto.tabCoverImageUrl().contains("covers/cover.png"));
        assertTrue(dto.completedByMe());
    }

    @Test
    void dailyChallengeDtoFromWithNullOwnerAndNullCoverTest() {
        PdfDocument pdf = PdfDocument.builder()
                .id(10L)
                .songTitle("Song")
                .description("Desc")
                .coverImagePath(null)
                .owner(null)
                .build();
        DailyChallenge challenge = DailyChallenge.builder()
                .id(2L)
                .challengeDate(LocalDate.of(2024, 6, 1))
                .difficulty(Level.PRINCIPIANTE)
                .xpReward(15)
                .pdfDocument(pdf)
                .build();

        DailyChallengeDto dto = DailyChallengeDto.from(challenge, false);

        assertNull(dto.tabOwnerName());
        assertNull(dto.tabCoverImageUrl());
        assertFalse(dto.completedByMe());
    }
}
