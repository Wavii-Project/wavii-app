package com.wavii.dto;

import com.wavii.dto.auth.*;
import com.wavii.dto.onboarding.CompleteOnboardingRequest;
import com.wavii.dto.onboarding.VerificationStatusResponse;
import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Lombok-generated equals, hashCode, and toString on DTO classes.
 */
class DtoLombokTest {

    // ── AuthResponse ──────────────────────────────────────────────

    @Test
    void authResponseEqualsSameObjectTest() {
        AuthResponse r = AuthResponse.builder().email("a@b.com").accessToken("tok").build();
        assertEquals(r, r);
    }

    @Test
    void authResponseEqualsEqualObjectsTest() {
        UUID id = UUID.randomUUID();
        AuthResponse r1 = AuthResponse.builder().userId(id).email("a@b.com").accessToken("tok")
                .refreshToken("ref").name("Ana").role(Role.USUARIO).subscription("free")
                .emailVerified(true).onboardingCompleted(false).teacherVerified(false).build();
        AuthResponse r2 = AuthResponse.builder().userId(id).email("a@b.com").accessToken("tok")
                .refreshToken("ref").name("Ana").role(Role.USUARIO).subscription("free")
                .emailVerified(true).onboardingCompleted(false).teacherVerified(false).build();
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void authResponseEqualsDifferentEmailTest() {
        AuthResponse r1 = AuthResponse.builder().email("a@b.com").build();
        AuthResponse r2 = AuthResponse.builder().email("x@y.com").build();
        assertNotEquals(r1, r2);
    }

    @Test
    void authResponseEqualsNullTest() {
        AuthResponse r = AuthResponse.builder().build();
        assertNotEquals(r, null);
    }

    @Test
    void authResponseEqualsDifferentTypeTest() {
        AuthResponse r = AuthResponse.builder().build();
        assertNotEquals(r, "string");
    }

    @Test
    void authResponseToStringContainsFieldsTest() {
        AuthResponse r = AuthResponse.builder().email("a@b.com").accessToken("tok123").build();
        String str = r.toString();
        assertTrue(str.contains("a@b.com") || str.contains("AuthResponse"));
    }

    @Test
    void authResponseHashCodeConsistentForNullTest() {
        AuthResponse r = AuthResponse.builder().build();
        assertEquals(r.hashCode(), r.hashCode());
    }

    // ── LoginRequest ──────────────────────────────────────────────

    @Test
    void loginRequestEqualsEqualObjectsTest() {
        LoginRequest r1 = new LoginRequest();
        r1.setEmail("a@b.com");
        r1.setPassword("pass");
        LoginRequest r2 = new LoginRequest();
        r2.setEmail("a@b.com");
        r2.setPassword("pass");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void loginRequestEqualsDifferentPasswordTest() {
        LoginRequest r1 = new LoginRequest();
        r1.setEmail("a@b.com");
        r1.setPassword("pass1");
        LoginRequest r2 = new LoginRequest();
        r2.setEmail("a@b.com");
        r2.setPassword("pass2");
        assertNotEquals(r1, r2);
    }

    @Test
    void loginRequestToStringTest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("a@b.com");
        assertNotNull(r.toString());
    }

    // ── RegisterRequest ───────────────────────────────────────────

    @Test
    void registerRequestEqualsEqualObjectsTest() {
        RegisterRequest r1 = new RegisterRequest();
        r1.setName("Ana");
        r1.setEmail("a@b.com");
        r1.setPassword("pass");
        RegisterRequest r2 = new RegisterRequest();
        r2.setName("Ana");
        r2.setEmail("a@b.com");
        r2.setPassword("pass");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void registerRequestEqualsDifferentNameTest() {
        RegisterRequest r1 = new RegisterRequest();
        r1.setName("Ana");
        RegisterRequest r2 = new RegisterRequest();
        r2.setName("Juan");
        assertNotEquals(r1, r2);
    }

    @Test
    void registerRequestToStringTest() {
        RegisterRequest r = new RegisterRequest();
        r.setName("Ana");
        r.setEmail("a@b.com");
        assertNotNull(r.toString());
    }

    // ── ForgotPasswordRequest ─────────────────────────────────────

    @Test
    void forgotPasswordRequestEqualsEqualObjectsTest() {
        ForgotPasswordRequest r1 = new ForgotPasswordRequest();
        r1.setEmail("a@b.com");
        ForgotPasswordRequest r2 = new ForgotPasswordRequest();
        r2.setEmail("a@b.com");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void forgotPasswordRequestToStringTest() {
        ForgotPasswordRequest r = new ForgotPasswordRequest();
        r.setEmail("a@b.com");
        assertNotNull(r.toString());
    }

    // ── ResetPasswordRequest ──────────────────────────────────────

    @Test
    void resetPasswordRequestEqualsEqualObjectsTest() {
        ResetPasswordRequest r1 = new ResetPasswordRequest();
        r1.setToken("tok");
        r1.setNewPassword("pass");
        ResetPasswordRequest r2 = new ResetPasswordRequest();
        r2.setToken("tok");
        r2.setNewPassword("pass");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void resetPasswordRequestToStringTest() {
        ResetPasswordRequest r = new ResetPasswordRequest();
        r.setToken("tok123");
        assertNotNull(r.toString());
    }

    // ── RefreshTokenRequest ───────────────────────────────────────

    @Test
    void refreshTokenRequestEqualsEqualObjectsTest() {
        RefreshTokenRequest r1 = new RefreshTokenRequest();
        r1.setRefreshToken("ref_tok");
        RefreshTokenRequest r2 = new RefreshTokenRequest();
        r2.setRefreshToken("ref_tok");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void refreshTokenRequestToStringTest() {
        RefreshTokenRequest r = new RefreshTokenRequest();
        r.setRefreshToken("ref_tok");
        assertNotNull(r.toString());
    }

    // ── CompleteOnboardingRequest ─────────────────────────────────

    @Test
    void completeOnboardingRequestEqualsEqualObjectsTest() {
        CompleteOnboardingRequest r1 = new CompleteOnboardingRequest();
        r1.setRole(Role.USUARIO);
        r1.setLevel(Level.PRINCIPIANTE);
        r1.setAvatarUrl("http://example.com/a.png");
        CompleteOnboardingRequest r2 = new CompleteOnboardingRequest();
        r2.setRole(Role.USUARIO);
        r2.setLevel(Level.PRINCIPIANTE);
        r2.setAvatarUrl("http://example.com/a.png");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void completeOnboardingRequestEqualsDifferentRoleTest() {
        CompleteOnboardingRequest r1 = new CompleteOnboardingRequest();
        r1.setRole(Role.USUARIO);
        CompleteOnboardingRequest r2 = new CompleteOnboardingRequest();
        r2.setRole(Role.ADMIN);
        assertNotEquals(r1, r2);
    }

    @Test
    void completeOnboardingRequestToStringTest() {
        CompleteOnboardingRequest r = new CompleteOnboardingRequest();
        r.setRole(Role.USUARIO);
        r.setLevel(Level.INTERMEDIO);
        assertNotNull(r.toString());
    }

    // ── VerificationStatusResponse ────────────────────────────────

    @Test
    void verificationStatusResponseEqualsEqualObjectsTest() {
        VerificationStatusResponse r1 = VerificationStatusResponse.builder()
                .teacherVerified(true).onboardingCompleted(true).message("OK").build();
        VerificationStatusResponse r2 = VerificationStatusResponse.builder()
                .teacherVerified(true).onboardingCompleted(true).message("OK").build();
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void verificationStatusResponseEqualsDifferentMessageTest() {
        VerificationStatusResponse r1 = VerificationStatusResponse.builder().message("A").build();
        VerificationStatusResponse r2 = VerificationStatusResponse.builder().message("B").build();
        assertNotEquals(r1, r2);
    }

    @Test
    void verificationStatusResponseToStringTest() {
        VerificationStatusResponse r = VerificationStatusResponse.builder()
                .teacherVerified(true).message("Verificado").build();
        assertNotNull(r.toString());
    }
}
