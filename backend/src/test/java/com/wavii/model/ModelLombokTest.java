package com.wavii.model;

import com.wavii.model.enums.ForumCategory;
import com.wavii.model.enums.ForumMembershipRole;
import com.wavii.model.enums.Level;
import com.wavii.model.enums.ListingType;
import com.wavii.model.enums.MusicalGenre;
import com.wavii.model.enums.MusicianRole;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import com.wavii.model.enums.VerificationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Lombok-generated equals, hashCode, and toString on entity classes.
 */
class ModelLombokTest {

    // ── User ──────────────────────────────────────────────────────

    @Test
    void userEqualsSameObjectTest() {
        User u = User.builder().email("a@b.com").name("Ana").build();
        assertEquals(u, u);
    }

    @Test
    void userEqualsEqualObjectsTest() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        User u1 = User.builder().id(id).name("Ana").email("a@b.com")
                .passwordHash("hash").role(Role.USUARIO).subscription(Subscription.FREE)
                .emailVerified(false).onboardingCompleted(false).teacherVerified(false)
                .xp(0).streak(0).createdAt(now).build();
        User u2 = User.builder().id(id).name("Ana").email("a@b.com")
                .passwordHash("hash").role(Role.USUARIO).subscription(Subscription.FREE)
                .emailVerified(false).onboardingCompleted(false).teacherVerified(false)
                .xp(0).streak(0).createdAt(now).build();
        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }

    @Test
    void userEqualsDifferentEmailTest() {
        User u1 = User.builder().email("a@b.com").name("Ana").build();
        User u2 = User.builder().email("x@y.com").name("Ana").build();
        assertNotEquals(u1, u2);
    }

    @Test
    void userEqualsNullTest() {
        User u = User.builder().email("a@b.com").name("Ana").build();
        assertNotEquals(u, null);
    }

    @Test
    void userEqualsDifferentTypeTest() {
        User u = User.builder().email("a@b.com").name("Ana").build();
        assertNotEquals(u, "string");
    }

    @Test
    void userHashCodeConsistentTest() {
        User u = User.builder().email("a@b.com").name("Ana").build();
        assertEquals(u.hashCode(), u.hashCode());
    }

    @Test
    void userToStringNotNullTest() {
        User u = User.builder().email("a@b.com").name("Ana").role(Role.USUARIO).build();
        assertNotNull(u.toString());
        assertTrue(u.toString().contains("User") || u.toString().contains("a@b.com"));
    }

    @Test
    void userEqualsWithXpAndStreakTest() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        User u1 = User.builder().email("a@b.com").name("Ana").xp(100).streak(5).createdAt(now).build();
        User u2 = User.builder().email("a@b.com").name("Ana").xp(100).streak(5).createdAt(now).build();
        assertEquals(u1, u2);
    }

    @Test
    void userEqualsDifferentXpTest() {
        User u1 = User.builder().email("a@b.com").name("Ana").xp(100).build();
        User u2 = User.builder().email("a@b.com").name("Ana").xp(200).build();
        assertNotEquals(u1, u2);
    }

    @Test
    void userEqualsWithNullGoogleIdTest() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        User u1 = User.builder().email("a@b.com").name("Ana").googleId(null).createdAt(now).build();
        User u2 = User.builder().email("a@b.com").name("Ana").googleId(null).createdAt(now).build();
        assertEquals(u1, u2);
    }

    @Test
    void userEqualsWithStripeFieldsTest() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        User u1 = User.builder().email("a@b.com").name("Ana")
                .stripeCustomerId("cus_123").stripeSubscriptionId("sub_123")
                .subscriptionStatus("active").createdAt(now).build();
        User u2 = User.builder().email("a@b.com").name("Ana")
                .stripeCustomerId("cus_123").stripeSubscriptionId("sub_123")
                .subscriptionStatus("active").createdAt(now).build();
        assertEquals(u1, u2);
    }

    @Test
    void userEqualsWithBulletinFieldsTest() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        User u1 = User.builder().email("a@b.com").name("Ana")
                .bio("Bio text").instrument("Piano").pricePerHour(new BigDecimal("30.00")).createdAt(now).build();
        User u2 = User.builder().email("a@b.com").name("Ana")
                .bio("Bio text").instrument("Piano").pricePerHour(new BigDecimal("30.00")).createdAt(now).build();
        assertEquals(u1, u2);
    }

    // ── VerificationToken ─────────────────────────────────────────

    @Test
    void verificationTokenEqualsEqualObjectsTest() {
        UUID id = UUID.randomUUID();
        LocalDateTime expires = LocalDateTime.now().plusHours(1);
        User user = User.builder().email("a@b.com").name("Ana").build();
        VerificationToken t1 = new VerificationToken(id, "tok", user, "EMAIL_VERIFICATION", expires, false);
        VerificationToken t2 = new VerificationToken(id, "tok", user, "EMAIL_VERIFICATION", expires, false);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void verificationTokenEqualsDifferentTokenTest() {
        VerificationToken t1 = VerificationToken.builder().token("tok1").type("EMAIL_VERIFICATION")
                .expiresAt(LocalDateTime.now()).build();
        VerificationToken t2 = VerificationToken.builder().token("tok2").type("EMAIL_VERIFICATION")
                .expiresAt(LocalDateTime.now()).build();
        assertNotEquals(t1, t2);
    }

    @Test
    void verificationTokenToStringNotNullTest() {
        VerificationToken t = VerificationToken.builder().token("tok").type("EMAIL_VERIFICATION")
                .expiresAt(LocalDateTime.now()).build();
        assertNotNull(t.toString());
    }

    @Test
    void verificationTokenEqualsSameObjectTest() {
        VerificationToken t = VerificationToken.builder().token("tok").type("EMAIL_VERIFICATION")
                .expiresAt(LocalDateTime.now()).build();
        assertEquals(t, t);
    }

    @Test
    void verificationTokenEqualsNullTest() {
        VerificationToken t = VerificationToken.builder().token("tok").type("EMAIL_VERIFICATION")
                .expiresAt(LocalDateTime.now()).build();
        assertNotEquals(t, null);
    }

    // ── VerificationRequest ───────────────────────────────────────

    @Test
    void verificationRequestEqualsEqualObjectsTest() {
        UUID id = UUID.randomUUID();
        User user = User.builder().email("a@b.com").name("Ana").build();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        VerificationRequest r1 = new VerificationRequest(id, user, "file.pdf",
                "/path/file.pdf", VerificationStatus.PENDING, now);
        VerificationRequest r2 = new VerificationRequest(id, user, "file.pdf",
                "/path/file.pdf", VerificationStatus.PENDING, now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void verificationRequestEqualsDifferentStatusTest() {
        VerificationRequest r1 = VerificationRequest.builder()
                .fileName("f.pdf").filePath("/p").status(VerificationStatus.PENDING).build();
        VerificationRequest r2 = VerificationRequest.builder()
                .fileName("f.pdf").filePath("/p").status(VerificationStatus.APPROVED).build();
        assertNotEquals(r1, r2);
    }

    @Test
    void verificationRequestToStringNotNullTest() {
        VerificationRequest r = VerificationRequest.builder()
                .fileName("file.pdf").filePath("/path").status(VerificationStatus.PENDING).build();
        assertNotNull(r.toString());
    }

    @Test
    void verificationRequestEqualsSameObjectTest() {
        VerificationRequest r = VerificationRequest.builder()
                .fileName("file.pdf").filePath("/path").status(VerificationStatus.PENDING).build();
        assertEquals(r, r);
    }

    @Test
    void verificationRequestEqualsNullTest() {
        VerificationRequest r = VerificationRequest.builder()
                .fileName("file.pdf").filePath("/path").status(VerificationStatus.PENDING).build();
        assertNotEquals(r, null);
    }

    // ── BandListing ───────────────────────────────────────────────

    @Test
    void bandListingEqualsEqualObjectsTest() {
        User creator = User.builder().email("a@b.com").name("Ana").build();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        BandListing b1 = new BandListing(id, "Band", "Desc", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), creator, "contact",
                null, List.of(), now);
        BandListing b2 = new BandListing(id, "Band", "Desc", ListingType.BANDA_BUSCA_MUSICOS,
                MusicalGenre.ROCK, "Madrid", List.of(MusicianRole.GUITARRISTA), creator, "contact",
                null, List.of(), now);
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    @Test
    void bandListingEqualsDifferentTitleTest() {
        BandListing b1 = BandListing.builder().title("Band A").city("Madrid")
                .genre(MusicalGenre.ROCK).type(ListingType.BANDA_BUSCA_MUSICOS).build();
        BandListing b2 = BandListing.builder().title("Band B").city("Madrid")
                .genre(MusicalGenre.ROCK).type(ListingType.BANDA_BUSCA_MUSICOS).build();
        assertNotEquals(b1, b2);
    }

    @Test
    void bandListingEqualsSameObjectTest() {
        BandListing b = BandListing.builder().title("Band").build();
        assertEquals(b, b);
    }

    @Test
    void bandListingEqualsNullTest() {
        BandListing b = BandListing.builder().title("Band").build();
        assertNotEquals(b, null);
    }

    @Test
    void bandListingToStringNotNullTest() {
        BandListing b = BandListing.builder().title("Band").genre(MusicalGenre.ROCK).build();
        assertNotNull(b.toString());
        assertTrue(b.toString().contains("BandListing") || b.toString().contains("Band"));
    }

    @Test
    void bandListingOnCreateSetsCreatedAtTest() {
        BandListing b = BandListing.builder()
                .title("Band").genre(MusicalGenre.ROCK)
                .type(ListingType.BANDA_BUSCA_MUSICOS).city("Madrid").build();
        b.onCreate();
        assertNotNull(b.getCreatedAt());
    }

    // ── Forum ─────────────────────────────────────────────────────

    @Test
    void forumEqualsEqualObjectsTest() {
        User creator = User.builder().email("a@b.com").name("Ana").build();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        Forum f1 = new Forum(id, "Forum", "Desc", ForumCategory.GENERAL, "http://img", "Madrid", creator, now, 1, 0);
        Forum f2 = new Forum(id, "Forum", "Desc", ForumCategory.GENERAL, "http://img", "Madrid", creator, now, 1, 0);
        assertEquals(f1, f2);
        assertEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    void forumEqualsDifferentNameTest() {
        Forum f1 = Forum.builder().name("Forum A").category(ForumCategory.GENERAL).build();
        Forum f2 = Forum.builder().name("Forum B").category(ForumCategory.GENERAL).build();
        assertNotEquals(f1, f2);
    }

    @Test
    void forumEqualsSameObjectTest() {
        Forum f = Forum.builder().name("Forum").category(ForumCategory.GENERAL).build();
        assertEquals(f, f);
    }

    @Test
    void forumEqualsNullTest() {
        Forum f = Forum.builder().name("Forum").category(ForumCategory.GENERAL).build();
        assertNotEquals(f, null);
    }

    @Test
    void forumToStringNotNullTest() {
        Forum f = Forum.builder().name("Forum").category(ForumCategory.GENERAL).build();
        assertNotNull(f.toString());
    }

    @Test
    void forumOnCreateSetsCreatedAtTest() {
        Forum f = Forum.builder().name("Forum").category(ForumCategory.GENERAL).build();
        f.onCreate();
        assertNotNull(f.getCreatedAt());
    }

    // ── ForumMembership ───────────────────────────────────────────

    @Test
    void forumMembershipEqualsEqualObjectsTest() {
        User user = User.builder().email("a@b.com").name("Ana").build();
        Forum forum = Forum.builder().name("Forum").category(ForumCategory.GENERAL).build();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        ForumMembership m1 = new ForumMembership(id, forum, user, ForumMembershipRole.MEMBER, now);
        ForumMembership m2 = new ForumMembership(id, forum, user, ForumMembershipRole.MEMBER, now);
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void forumMembershipEqualsSameObjectTest() {
        ForumMembership m = ForumMembership.builder().build();
        assertEquals(m, m);
    }

    @Test
    void forumMembershipEqualsNullTest() {
        ForumMembership m = ForumMembership.builder().build();
        assertNotEquals(m, null);
    }

    @Test
    void forumMembershipToStringNotNullTest() {
        ForumMembership m = ForumMembership.builder().build();
        assertNotNull(m.toString());
    }

    @Test
    void forumMembershipOnCreateSetsJoinedAtTest() {
        ForumMembership m = ForumMembership.builder().build();
        m.onCreate();
        assertNotNull(m.getJoinedAt());
    }

    // ── ForumPost ─────────────────────────────────────────────────

    @Test
    void forumPostEqualsEqualObjectsTest() {
        User user = User.builder().email("a@b.com").name("Ana").build();
        Forum forum = Forum.builder().name("Forum").category(ForumCategory.GENERAL).build();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        ForumPost p1 = new ForumPost(id, forum, user, "Content", now);
        ForumPost p2 = new ForumPost(id, forum, user, "Content", now);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void forumPostEqualsDifferentContentTest() {
        ForumPost p1 = ForumPost.builder().content("Hello").build();
        ForumPost p2 = ForumPost.builder().content("World").build();
        assertNotEquals(p1, p2);
    }

    @Test
    void forumPostEqualsSameObjectTest() {
        ForumPost p = ForumPost.builder().content("Hello").build();
        assertEquals(p, p);
    }

    @Test
    void forumPostToStringNotNullTest() {
        ForumPost p = ForumPost.builder().content("Hello").build();
        assertNotNull(p.toString());
    }

    @Test
    void forumPostOnCreateSetsCreatedAtTest() {
        ForumPost p = ForumPost.builder().content("Hello").build();
        p.onCreate();
        assertNotNull(p.getCreatedAt());
    }

    // ── PdfDocument ───────────────────────────────────────────────

    @Test
    void pdfDocumentEqualsEqualObjectsTest() {
        User owner = User.builder().email("a@b.com").name("Ana").build();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        PdfDocument d1 = new PdfDocument(1L, "t.pdf", "f.pdf", "/p", 1024L, 5, now, "Song", "Desc", "/covers/cover.png", 2, 0, owner);
        PdfDocument d2 = new PdfDocument(1L, "t.pdf", "f.pdf", "/p", 1024L, 5, now, "Song", "Desc", "/covers/cover.png", 2, 0, owner);
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    void pdfDocumentEqualsDifferentFileNameTest() {
        PdfDocument d1 = PdfDocument.builder().fileName("a.pdf").originalName("a.pdf")
                .filePath("/p").uploadedAt(LocalDateTime.now()).build();
        PdfDocument d2 = PdfDocument.builder().fileName("b.pdf").originalName("b.pdf")
                .filePath("/p").uploadedAt(LocalDateTime.now()).build();
        assertNotEquals(d1, d2);
    }

    @Test
    void pdfDocumentEqualsSameObjectTest() {
        PdfDocument d = PdfDocument.builder().fileName("a.pdf").build();
        assertEquals(d, d);
    }

    @Test
    void pdfDocumentEqualsNullTest() {
        PdfDocument d = PdfDocument.builder().fileName("a.pdf").build();
        assertNotEquals(d, null);
    }

    @Test
    void pdfDocumentToStringNotNullTest() {
        PdfDocument d = PdfDocument.builder().fileName("a.pdf").songTitle("Song").build();
        assertNotNull(d.toString());
    }

    // ── PdfLike ───────────────────────────────────────────────────

    @Test
    void pdfLikeEqualsEqualObjectsTest() {
        User user = User.builder().email("a@b.com").name("Ana").build();
        PdfDocument pdf = PdfDocument.builder().fileName("a.pdf").build();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        PdfLike l1 = new PdfLike(1L, pdf, user, now);
        PdfLike l2 = new PdfLike(1L, pdf, user, now);
        assertEquals(l1, l2);
        assertEquals(l1.hashCode(), l2.hashCode());
    }

    @Test
    void pdfLikeEqualsDifferentTimeTest() {
        PdfLike l1 = PdfLike.builder().likedAt(LocalDateTime.of(2024, 1, 1, 0, 0)).build();
        PdfLike l2 = PdfLike.builder().likedAt(LocalDateTime.of(2024, 1, 2, 0, 0)).build();
        assertNotEquals(l1, l2);
    }

    @Test
    void pdfLikeEqualsSameObjectTest() {
        PdfLike l = PdfLike.builder().build();
        assertEquals(l, l);
    }

    @Test
    void pdfLikeEqualsNullTest() {
        PdfLike l = PdfLike.builder().build();
        assertNotEquals(l, null);
    }

    @Test
    void pdfLikeToStringNotNullTest() {
        PdfLike l = PdfLike.builder().likedAt(LocalDateTime.now()).build();
        assertNotNull(l.toString());
    }

    // ── DailyChallenge ────────────────────────────────────────────

    @Test
    void dailyChallengeEqualsEqualObjectsTest() {
        PdfDocument pdf = PdfDocument.builder().fileName("a.pdf").build();
        DailyChallenge c1 = new DailyChallenge(1L, LocalDate.of(2024, 1, 1), Level.PRINCIPIANTE, 1, 15, pdf);
        DailyChallenge c2 = new DailyChallenge(1L, LocalDate.of(2024, 1, 1), Level.PRINCIPIANTE, 1, 15, pdf);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void dailyChallengeEqualsDifferentDifficultyTest() {
        DailyChallenge c1 = DailyChallenge.builder().difficulty(Level.PRINCIPIANTE).xpReward(15).build();
        DailyChallenge c2 = DailyChallenge.builder().difficulty(Level.AVANZADO).xpReward(50).build();
        assertNotEquals(c1, c2);
    }

    @Test
    void dailyChallengeEqualsSameObjectTest() {
        DailyChallenge c = DailyChallenge.builder().difficulty(Level.PRINCIPIANTE).build();
        assertEquals(c, c);
    }

    @Test
    void dailyChallengeEqualsNullTest() {
        DailyChallenge c = DailyChallenge.builder().difficulty(Level.PRINCIPIANTE).build();
        assertNotEquals(c, null);
    }

    @Test
    void dailyChallengeToStringNotNullTest() {
        DailyChallenge c = DailyChallenge.builder().difficulty(Level.PRINCIPIANTE).xpReward(15).build();
        assertNotNull(c.toString());
    }

    // ── UserChallengeCompletion ───────────────────────────────────

    @Test
    void userChallengeCompletionEqualsEqualObjectsTest() {
        User user = User.builder().email("a@b.com").name("Ana").build();
        PdfDocument pdf = PdfDocument.builder().fileName("a.pdf").build();
        DailyChallenge challenge = DailyChallenge.builder().difficulty(Level.PRINCIPIANTE).build();
        LocalDate today = LocalDate.of(2024, 1, 1);
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 10, 0);
        UserChallengeCompletion c1 = new UserChallengeCompletion(1L, user, challenge, today, now);
        UserChallengeCompletion c2 = new UserChallengeCompletion(1L, user, challenge, today, now);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void userChallengeCompletionEqualsDifferentDateTest() {
        UserChallengeCompletion c1 = UserChallengeCompletion.builder()
                .completedDate(LocalDate.of(2024, 1, 1)).build();
        UserChallengeCompletion c2 = UserChallengeCompletion.builder()
                .completedDate(LocalDate.of(2024, 1, 2)).build();
        assertNotEquals(c1, c2);
    }

    @Test
    void userChallengeCompletionEqualsSameObjectTest() {
        UserChallengeCompletion c = UserChallengeCompletion.builder()
                .completedDate(LocalDate.now()).build();
        assertEquals(c, c);
    }

    @Test
    void userChallengeCompletionEqualsNullTest() {
        UserChallengeCompletion c = UserChallengeCompletion.builder()
                .completedDate(LocalDate.now()).build();
        assertNotEquals(c, null);
    }

    @Test
    void userChallengeCompletionToStringNotNullTest() {
        UserChallengeCompletion c = UserChallengeCompletion.builder()
                .completedDate(LocalDate.now()).build();
        assertNotNull(c.toString());
    }
}
