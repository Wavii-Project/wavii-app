package com.wavii.model;

import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        LocalDate today = LocalDate.now();

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
        user.setAcceptsMessages(false);
        user.setTrialUsed(true);
        user.setSubscriptionCancelAtPeriodEnd(true);
        user.setSubscriptionCurrentPeriodEnd(now);
        user.setDeletionScheduledAt(now);
        user.setCity("Madrid");
        user.setProvince("Madrid");
        user.setLatitude(40.0);
        user.setLongitude(-3.0);
        user.setAddress("Calle 1");
        user.setContactEmail("contact@test.com");
        user.setContactPhone("123456789");
        user.setInstagramUrl("insta");
        user.setTiktokUrl("tik");
        user.setYoutubeUrl("yt");
        user.setFacebookUrl("fb");
        user.setBannerImageUrl("banner");
        user.setAvailabilityPreference("MORNING");
        user.setAvailabilityNotes("Notes");
        user.setPlaceImageUrls(java.util.List.of("url1", "url2"));
        user.setBestStreak(10);
        user.setLastStreakDate(today);
        user.setClassModality("ONLINE");

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
        assertFalse(user.isAcceptsMessages());
        assertTrue(user.isTrialUsed());
        assertTrue(user.isSubscriptionCancelAtPeriodEnd());
        assertEquals(now, user.getSubscriptionCurrentPeriodEnd());
        assertEquals(now, user.getDeletionScheduledAt());
        assertEquals("Madrid", user.getCity());
        assertEquals("Madrid", user.getProvince());
        assertEquals(40.0, user.getLatitude());
        assertEquals(-3.0, user.getLongitude());
        assertEquals("Calle 1", user.getAddress());
        assertEquals("contact@test.com", user.getContactEmail());
        assertEquals("123456789", user.getContactPhone());
        assertEquals("insta", user.getInstagramUrl());
        assertEquals("tik", user.getTiktokUrl());
        assertEquals("yt", user.getYoutubeUrl());
        assertEquals("fb", user.getFacebookUrl());
        assertEquals("banner", user.getBannerImageUrl());
        assertEquals("MORNING", user.getAvailabilityPreference());
        assertEquals("Notes", user.getAvailabilityNotes());
        assertEquals(2, user.getPlaceImageUrls().size());
        assertEquals(10, user.getBestStreak());
        assertEquals(today, user.getLastStreakDate());
        assertEquals("ONLINE", user.getClassModality());
    }

    @Test
    void equalsHashCodeToStringExhaustiveTest() throws Exception {
        UUID id = UUID.randomUUID();
        User u1 = User.builder()
                .id(id)
                .email("test@test.com")
                .name("Test")
                .role(Role.USUARIO)
                .createdAt(LocalDateTime.now())
                .build();
        User u2 = User.builder()
                .id(id)
                .email("test@test.com")
                .name("Test")
                .role(Role.USUARIO)
                .createdAt(u1.getCreatedAt())
                .build();

        ModelTestHelper.testEqualsAndHashCodeExhaustively(u1, u2, User.class);
    }


    @Test
    void allArgsConstructorTest() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        java.util.List<String> images = java.util.List.of("img1");

        User fullUser = new User(
            id, "Name", true, "email@test.com", "hash", Role.USUARIO, Subscription.FREE,
            Level.PRINCIPIANTE, true, true, true, "google", "avatar", "cus", "sub",
            "status", true, true, now, now, "bio", "piano", "city", 10.0, 20.0,
            "address", "prov", "cEmail", "cPhone", "insta", "tik", "yt", "fb",
            "banner", "pref", "notes", images, "ONLINE", BigDecimal.TEN,
            100, 5, 10, today, now, now
        );

        assertEquals(id, fullUser.getId());
        assertEquals("Name", fullUser.getName());
        assertTrue(fullUser.isAcceptsMessages());
        assertEquals("email@test.com", fullUser.getEmail());
        assertEquals(Role.USUARIO, fullUser.getRole());
        assertEquals(Subscription.FREE, fullUser.getSubscription());
        assertEquals(Level.PRINCIPIANTE, fullUser.getLevel());
        assertTrue(fullUser.isEmailVerified());
        assertTrue(fullUser.isOnboardingCompleted());
        assertTrue(fullUser.isTeacherVerified());
        assertEquals("google", fullUser.getGoogleId());
        assertEquals("avatar", fullUser.getAvatarUrl());
        assertEquals("cus", fullUser.getStripeCustomerId());
        assertEquals("sub", fullUser.getStripeSubscriptionId());
        assertEquals("status", fullUser.getSubscriptionStatus());
        assertTrue(fullUser.isTrialUsed());
        assertTrue(fullUser.isSubscriptionCancelAtPeriodEnd());
        assertEquals(now, fullUser.getSubscriptionCurrentPeriodEnd());
        assertEquals(now, fullUser.getDeletionScheduledAt());
        assertEquals("bio", fullUser.getBio());
        assertEquals("piano", fullUser.getInstrument());
        assertEquals("city", fullUser.getCity());
        assertEquals(10.0, fullUser.getLatitude());
        assertEquals(20.0, fullUser.getLongitude());
        assertEquals("address", fullUser.getAddress());
        assertEquals("prov", fullUser.getProvince());
        assertEquals("cEmail", fullUser.getContactEmail());
        assertEquals("cPhone", fullUser.getContactPhone());
        assertEquals("insta", fullUser.getInstagramUrl());
        assertEquals("tik", fullUser.getTiktokUrl());
        assertEquals("yt", fullUser.getYoutubeUrl());
        assertEquals("fb", fullUser.getFacebookUrl());
        assertEquals("banner", fullUser.getBannerImageUrl());
        assertEquals("pref", fullUser.getAvailabilityPreference());
        assertEquals("notes", fullUser.getAvailabilityNotes());
        assertEquals(images, fullUser.getPlaceImageUrls());
        assertEquals("ONLINE", fullUser.getClassModality());
        assertEquals(BigDecimal.TEN, fullUser.getPricePerHour());
        assertEquals(100, fullUser.getXp());
        assertEquals(5, fullUser.getStreak());
        assertEquals(10, fullUser.getBestStreak());
        assertEquals(today, fullUser.getLastStreakDate());
        assertEquals(now, fullUser.getCreatedAt());
        assertEquals(now, fullUser.getLastLoginAt());
    }

    @Test
    void userBuilderCoverageTest() {
        User.builder()
            .id(UUID.randomUUID())
            .name("n")
            .acceptsMessages(true)
            .email("e")
            .passwordHash("p")
            .role(Role.USUARIO)
            .subscription(Subscription.FREE)
            .level(Level.PRINCIPIANTE)
            .emailVerified(true)
            .onboardingCompleted(true)
            .teacherVerified(true)
            .googleId("g")
            .avatarUrl("a")
            .stripeCustomerId("c")
            .stripeSubscriptionId("s")
            .subscriptionStatus("st")
            .trialUsed(true)
            .subscriptionCancelAtPeriodEnd(true)
            .subscriptionCurrentPeriodEnd(LocalDateTime.now())
            .deletionScheduledAt(LocalDateTime.now())
            .bio("b")
            .instrument("i")
            .city("c")
            .latitude(1.0)
            .longitude(2.0)
            .address("a")
            .province("p")
            .contactEmail("ce")
            .contactPhone("cp")
            .instagramUrl("iu")
            .tiktokUrl("tu")
            .youtubeUrl("yu")
            .facebookUrl("fu")
            .bannerImageUrl("bi")
            .availabilityPreference("ap")
            .availabilityNotes("an")
            .placeImageUrls(new ArrayList<>())
            .classModality("cm")
            .pricePerHour(BigDecimal.TEN)
            .xp(1)
            .streak(2)
            .bestStreak(3)
            .lastStreakDate(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .lastLoginAt(LocalDateTime.now())
            .build();
    }
}
