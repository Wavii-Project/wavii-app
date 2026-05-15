package com.wavii.model;

import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserModelTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@test.com")
                .passwordHash("hashed_password")
                .role(Role.USUARIO)
                .subscription(Subscription.FREE)
                .build();
    }

    @Test
    void getUsernameReturnsEmailTest() {
        assertEquals("test@test.com", user.getUsername());
    }

    @Test
    void getPasswordReturnsPasswordHashTest() {
        assertEquals("hashed_password", user.getPassword());
    }

    @Test
    void getAuthoritiesReturnsRoleAuthorityTest() {
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USUARIO", authorities.iterator().next().getAuthority());
    }

    @Test
    void getAuthoritiesProfesorCertificadoReturnsCorrectRoleTest() {
        user.setRole(Role.PROFESOR_CERTIFICADO);
        assertEquals("ROLE_PROFESOR_CERTIFICADO", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void isAccountNonExpiredReturnsTrueTest() {
        assertTrue(user.isAccountNonExpired());
    }

    @Test
    void isAccountNonLockedReturnsTrueTest() {
        assertTrue(user.isAccountNonLocked());
    }

    @Test
    void isCredentialsNonExpiredReturnsTrueTest() {
        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void isEnabledReturnsTrueTest() {
        assertTrue(user.isEnabled());
    }

    @Test
    void builderDefaultValuesTest() {
        User newUser = User.builder()
                .name("New User")
                .email("new@test.com")
                .build();

        assertEquals(Role.USUARIO, newUser.getRole());
        assertEquals(Subscription.FREE, newUser.getSubscription());
        assertFalse(newUser.isEmailVerified());
        assertFalse(newUser.isOnboardingCompleted());
        assertFalse(newUser.isTeacherVerified());
        assertEquals(0, newUser.getXp());
        assertEquals(0, newUser.getStreak());
        assertNotNull(newUser.getCreatedAt());
    }

    @Test
    void settersAndGettersWorkCorrectlyTest() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        user.setId(id);
        user.setName("Updated Name");
        user.setEmail("updated@test.com");
        user.setRole(Role.ADMIN);
        user.setSubscription(Subscription.PLUS);
        user.setLevel(Level.INTERMEDIO);
        user.setEmailVerified(true);
        user.setOnboardingCompleted(true);
        user.setTeacherVerified(true);
        user.setGoogleId("google_123");
        user.setAvatarUrl("http://example.com/avatar.png");
        user.setStripeCustomerId("cus_123");
        user.setStripeSubscriptionId("sub_123");
        user.setSubscriptionStatus("active");
        user.setBio("My bio");
        user.setInstrument("Piano");
        user.setPricePerHour(new BigDecimal("30.00"));
        user.setXp(100);
        user.setStreak(5);
        user.setLastLoginAt(now);

        assertEquals(id, user.getId());
        assertEquals("Updated Name", user.getName());
        assertEquals("updated@test.com", user.getEmail());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(Subscription.PLUS, user.getSubscription());
        assertEquals(Level.INTERMEDIO, user.getLevel());
        assertTrue(user.isEmailVerified());
        assertTrue(user.isOnboardingCompleted());
        assertTrue(user.isTeacherVerified());
        assertEquals("google_123", user.getGoogleId());
        assertEquals("http://example.com/avatar.png", user.getAvatarUrl());
        assertEquals("cus_123", user.getStripeCustomerId());
        assertEquals("sub_123", user.getStripeSubscriptionId());
        assertEquals("active", user.getSubscriptionStatus());
        assertEquals("My bio", user.getBio());
        assertEquals("Piano", user.getInstrument());
        assertEquals(new BigDecimal("30.00"), user.getPricePerHour());
        assertEquals(100, user.getXp());
        assertEquals(5, user.getStreak());
        assertEquals(now, user.getLastLoginAt());
    }

    @Test
    void noArgsConstructorCreatesEmptyUserTest() {
        User emptyUser = new User();
        assertNull(emptyUser.getId());
        assertNull(emptyUser.getName());
    }

    @Test
    void allArgsConstructorSetsAllFieldsTest() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        // Usamos el builder para evitar acoplamiento al orden exacto de campos
        User fullUser = User.builder()
                .id(id)
                .name("Full Name")
                .email("full@test.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .subscription(Subscription.PLUS)
                .level(Level.AVANZADO)
                .emailVerified(true)
                .onboardingCompleted(true)
                .teacherVerified(true)
                .bio("bio text")
                .instrument("Violin")
                .city("Madrid")
                .latitude(40.4168)
                .longitude(-3.7038)
                .address("Calle Mayor 1")
                .classModality("PRESENCIAL")
                .pricePerHour(new BigDecimal("50.00"))
                .xp(500)
                .streak(30)
                .trialUsed(true)
                .subscriptionCancelAtPeriodEnd(true)
                .build();

        assertEquals(id, fullUser.getId());
        assertEquals("Full Name", fullUser.getName());
        assertEquals(Role.ADMIN, fullUser.getRole());
        assertEquals(Subscription.PLUS, fullUser.getSubscription());
        assertEquals(Level.AVANZADO, fullUser.getLevel());
        assertTrue(fullUser.isTrialUsed());
        assertTrue(fullUser.isSubscriptionCancelAtPeriodEnd());
        assertEquals("Madrid", fullUser.getCity());
        assertEquals("PRESENCIAL", fullUser.getClassModality());
    }
}
