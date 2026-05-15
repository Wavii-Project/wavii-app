package com.wavii.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    // ── Role ─────────────────────────────────────────────────────

    @Test
    void roleAllValuesExistTest() {
        assertNotNull(Role.USUARIO);
        assertNotNull(Role.PROFESOR_PARTICULAR);
        assertNotNull(Role.PROFESOR_CERTIFICADO);
        assertNotNull(Role.ADMIN);
    }

    @Test
    void roleValuesReturnAllTest() {
        Role[] roles = Role.values();
        assertEquals(4, roles.length);
    }

    @Test
    void roleValueOfReturnsCorrectTest() {
        assertEquals(Role.USUARIO, Role.valueOf("USUARIO"));
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
        assertEquals(Role.PROFESOR_PARTICULAR, Role.valueOf("PROFESOR_PARTICULAR"));
        assertEquals(Role.PROFESOR_CERTIFICADO, Role.valueOf("PROFESOR_CERTIFICADO"));
    }

    // ── Subscription ──────────────────────────────────────────────

    @Test
    void subscriptionAllValuesExistTest() {
        assertNotNull(Subscription.FREE);
        assertNotNull(Subscription.PLUS);
        assertNotNull(Subscription.EDUCATION);
    }

    @Test
    void subscriptionValuesReturnAllTest() {
        Subscription[] subs = Subscription.values();
        assertEquals(3, subs.length);
    }

    @Test
    void subscriptionValueOfReturnsCorrectTest() {
        assertEquals(Subscription.FREE, Subscription.valueOf("FREE"));
        assertEquals(Subscription.PLUS, Subscription.valueOf("PLUS"));
        assertEquals(Subscription.EDUCATION, Subscription.valueOf("EDUCATION"));
    }

    // ── Level ─────────────────────────────────────────────────────

    @Test
    void levelValuesExistTest() {
        Level[] levels = Level.values();
        assertTrue(levels.length > 0);
    }

    @Test
    void levelValueOfWorksForEachValueTest() {
        for (Level level : Level.values()) {
            assertEquals(level, Level.valueOf(level.name()));
        }
    }

    // ── VerificationStatus ────────────────────────────────────────

    @Test
    void verificationStatusAllValuesExistTest() {
        assertNotNull(VerificationStatus.PENDING);
        assertNotNull(VerificationStatus.APPROVED);
        assertNotNull(VerificationStatus.REJECTED);
    }

    @Test
    void verificationStatusValuesReturnAllTest() {
        VerificationStatus[] statuses = VerificationStatus.values();
        assertEquals(3, statuses.length);
    }

    @Test
    void verificationStatusValueOfReturnsCorrectTest() {
        assertEquals(VerificationStatus.PENDING, VerificationStatus.valueOf("PENDING"));
        assertEquals(VerificationStatus.APPROVED, VerificationStatus.valueOf("APPROVED"));
        assertEquals(VerificationStatus.REJECTED, VerificationStatus.valueOf("REJECTED"));
    }

    // ── Name/ordinal checks ───────────────────────────────────────

    @Test
    void roleNameReturnsCorrectStringTest() {
        assertEquals("USUARIO", Role.USUARIO.name());
        assertEquals("ADMIN", Role.ADMIN.name());
    }

    @Test
    void subscriptionNameReturnsCorrectStringTest() {
        assertEquals("FREE", Subscription.FREE.name());
        assertEquals("PLUS", Subscription.PLUS.name());
        assertEquals("EDUCATION", Subscription.EDUCATION.name());
    }

    @Test
    void verificationStatusNameReturnsCorrectStringTest() {
        assertEquals("PENDING", VerificationStatus.PENDING.name());
        assertEquals("APPROVED", VerificationStatus.APPROVED.name());
        assertEquals("REJECTED", VerificationStatus.REJECTED.name());
    }
}
