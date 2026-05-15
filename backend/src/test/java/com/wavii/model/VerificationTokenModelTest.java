package com.wavii.model;

import com.wavii.model.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationTokenModelTest {

    @Test
    void builderSetsAllFieldsTest() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setEmail("test@test.com");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        VerificationToken token = VerificationToken.builder()
                .id(id)
                .token("abc-123-xyz")
                .user(user)
                .type("EMAIL_VERIFICATION")
                .expiresAt(expiresAt)
                .used(false)
                .build();

        assertEquals(id, token.getId());
        assertEquals("abc-123-xyz", token.getToken());
        assertEquals(user, token.getUser());
        assertEquals("EMAIL_VERIFICATION", token.getType());
        assertEquals(expiresAt, token.getExpiresAt());
        assertFalse(token.isUsed());
    }

    @Test
    void builderDefaultUsedValueIsFalseTest() {
        VerificationToken token = VerificationToken.builder()
                .token("token-xyz")
                .type("PASSWORD_RESET")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        assertFalse(token.isUsed());
    }

    @Test
    void settersUpdateFieldsTest() {
        VerificationToken token = new VerificationToken();
        UUID id = UUID.randomUUID();
        User user = new User();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

        token.setId(id);
        token.setToken("new-token");
        token.setUser(user);
        token.setType("EMAIL_VERIFICATION");
        token.setExpiresAt(expiresAt);
        token.setUsed(true);

        assertEquals(id, token.getId());
        assertEquals("new-token", token.getToken());
        assertEquals(user, token.getUser());
        assertEquals("EMAIL_VERIFICATION", token.getType());
        assertEquals(expiresAt, token.getExpiresAt());
        assertTrue(token.isUsed());
    }

    @Test
    void allArgsConstructorSetsAllFieldsTest() {
        UUID id = UUID.randomUUID();
        User user = new User();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        VerificationToken token = new VerificationToken(id, "token-value", user,
                "PASSWORD_RESET", expiresAt, true);

        assertEquals(id, token.getId());
        assertEquals("token-value", token.getToken());
        assertEquals(user, token.getUser());
        assertEquals("PASSWORD_RESET", token.getType());
        assertEquals(expiresAt, token.getExpiresAt());
        assertTrue(token.isUsed());
    }

    @Test
    void noArgsConstructorCreatesEmptyTokenTest() {
        VerificationToken token = new VerificationToken();
        assertNull(token.getId());
        assertNull(token.getToken());
        assertFalse(token.isUsed());
    }
}
