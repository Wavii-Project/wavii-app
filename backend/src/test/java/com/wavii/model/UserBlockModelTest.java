package com.wavii.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserBlock model: Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor.
 */
class UserBlockModelTest {

    private User buildUser(String email) {
        return User.builder().email(email).name("User " + email).build();
    }

    // ── Constructor & Builder ─────────────────────────────────────

    @Test
    void userBlockNoArgsConstructorCreatesInstanceTest() {
        UserBlock block = new UserBlock();
        assertNotNull(block);
    }

    @Test
    void userBlockBuilderSetsAllFieldsTest() {
        User blocker = buildUser("blocker@test.com");
        User blocked = buildUser("blocked@test.com");
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);

        UserBlock block = UserBlock.builder()
                .id(1L)
                .blocker(blocker)
                .blocked(blocked)
                .createdAt(now)
                .build();

        assertEquals(1L, block.getId());
        assertEquals(blocker, block.getBlocker());
        assertEquals(blocked, block.getBlocked());
        assertEquals(now, block.getCreatedAt());
    }

    @Test
    void userBlockAllArgsConstructorTest() {
        User blocker = buildUser("b1@t.com");
        User blocked = buildUser("b2@t.com");
        LocalDateTime now = LocalDateTime.now();

        UserBlock block = new UserBlock(10L, blocker, blocked, now);

        assertEquals(10L, block.getId());
        assertEquals(blocker, block.getBlocker());
        assertEquals(blocked, block.getBlocked());
        assertEquals(now, block.getCreatedAt());
    }

    // ── Builder defaults ──────────────────────────────────────────

    @Test
    void userBlockBuilderDefaultCreatedAtIsNotNullTest() {
        UserBlock block = UserBlock.builder()
                .blocker(buildUser("a@t.com"))
                .blocked(buildUser("b@t.com"))
                .build();

        assertNotNull(block.getCreatedAt());
    }

    // ── Setters / Getters (via @Data) ─────────────────────────────

    @Test
    void userBlockSettersAndGettersWorkTest() {
        UserBlock block = new UserBlock();
        User blocker = buildUser("blocker@test.com");
        User blocked = buildUser("blocked@test.com");

        block.setId(7L);
        block.setBlocker(blocker);
        block.setBlocked(blocked);

        assertEquals(7L, block.getId());
        assertEquals(blocker, block.getBlocker());
        assertEquals(blocked, block.getBlocked());
    }

    // ── equals, hashCode, toString ────────────────────────────────

    @Test
    void userBlockEqualsSameObjectTest() {
        UserBlock block = UserBlock.builder().build();
        assertEquals(block, block);
    }

    @Test
    void userBlockEqualsEqualObjectsTest() {
        User blocker = buildUser("bl@t.com");
        User blocked = buildUser("bd@t.com");
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        UserBlock b1 = new UserBlock(1L, blocker, blocked, now);
        UserBlock b2 = new UserBlock(1L, blocker, blocked, now);

        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    @Test
    void userBlockEqualsDifferentIdTest() {
        User blocker = buildUser("bl@t.com");
        User blocked = buildUser("bd@t.com");
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        UserBlock b1 = new UserBlock(1L, blocker, blocked, now);
        UserBlock b2 = new UserBlock(2L, blocker, blocked, now);

        assertNotEquals(b1, b2);
    }

    @Test
    void userBlockEqualsNullTest() {
        UserBlock block = UserBlock.builder().build();
        assertNotEquals(block, null);
    }

    @Test
    void userBlockEqualsDifferentTypeTest() {
        UserBlock block = UserBlock.builder().build();
        assertNotEquals(block, "string");
    }

    @Test
    void userBlockHashCodeConsistentTest() {
        UserBlock block = UserBlock.builder().build();
        assertEquals(block.hashCode(), block.hashCode());
    }

    @Test
    void userBlockToStringNotNullTest() {
        UserBlock block = UserBlock.builder()
                .blocker(buildUser("a@t.com"))
                .blocked(buildUser("b@t.com"))
                .build();
        assertNotNull(block.toString());
        assertTrue(block.toString().contains("UserBlock") || block.toString().contains("null"));
    }
}
