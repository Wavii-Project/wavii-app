package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.model.UserBlock;
import com.wavii.model.UserReport;
import com.wavii.model.enums.Role;
import com.wavii.repository.PdfDocumentRepository;
import com.wavii.repository.UserBlockRepository;
import com.wavii.repository.UserRepository;
import com.wavii.repository.UserReportRepository;
import com.wavii.service.OdooService;
import com.wavii.service.PdfStorageService;
import com.wavii.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserReportRepository userReportRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private PdfDocumentRepository pdfDocumentRepository;

    @Mock
    private PdfStorageService pdfStorageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StripeService stripeService;

    @Mock
    private OdooService odooService;

    @InjectMocks
    private UserController userController;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setName("Test User");
        currentUser.setEmail("test@test.com");
    }

    // @Test
    // void updateMe_ValidName_ReturnsOk() {
    //     when(userRepository.save(currentUser)).thenReturn(currentUser);
    //     ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "Nuevo Nombre"));
    //     assertEquals(HttpStatus.OK, result.getStatusCode());
    //     assertNotNull(result.getBody());
    //     Map<?, ?> body = (Map<?, ?>) result.getBody();
    //     assertEquals("Nuevo Nombre", body.get("name"));
    //     verify(userRepository).save(currentUser);
    // }

    // @Test
    // void updateMe_NameWithSpaces_TrimsAndSaves() {
    //     when(userRepository.save(currentUser)).thenReturn(currentUser);
    //     ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "  Juan Pedro  "));
    //     assertEquals(HttpStatus.OK, result.getStatusCode());
    //     assertEquals("Juan Pedro", currentUser.getName());
    // }

    @Test
    void updateMeNullNameReturnsBadRequestTest() {
        Map<String, String> body = new java.util.HashMap<>();
        body.put("name", null);

        ResponseEntity<?> result = userController.updateMe(currentUser, body);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateMeBlankNameReturnsBadRequestTest() {
        ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "   "));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateMeTooShortNameReturnsBadRequestTest() {
        ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "Ab"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(userRepository);
    }

    // @Test
    // void updateMe_ExactlyThreeChars_ReturnsOk() {
    //     when(userRepository.save(currentUser)).thenReturn(currentUser);
    //     ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "Ana"));
    //     assertEquals(HttpStatus.OK, result.getStatusCode());
    // }

    // @Test
    // void deleteMe_Success_ReturnsOk() {
    //     when(stripeService.isConfigured()).thenReturn(false);
    //     when(userRepository.save(currentUser)).thenReturn(currentUser);
    //     ResponseEntity<?> result = userController.deleteMe(currentUser);
    //     assertEquals(HttpStatus.OK, result.getStatusCode());
    //     assertNotNull(result.getBody());
    //     verify(userRepository).save(currentUser);
    // }

    @Test
    void deleteMeExceptionReturnsInternalServerErrorTest() {
        when(stripeService.isConfigured()).thenReturn(false);
        when(userRepository.save(currentUser)).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> result = userController.deleteMe(currentUser);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    // ─── updateMe additional branches ────────────────────────────────────────

    @Test
    void updateMeValidNameSavesAndReturnsOkTest() {
        when(userRepository.existsByNameIgnoreCase("Juan")).thenReturn(false);
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "Juan"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("Juan", body.get("name"));
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateMeNameAlreadyTakenReturnsConflictTest() {
        when(userRepository.existsByNameIgnoreCase("OtroUser")).thenReturn(true);

        ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "OtroUser"));

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("CONFLICT", body.get("code"));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateMeSameNameIgnoreCaseAllowedTest() {
        // "Test User" is already the name of currentUser — same user, no conflict
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.updateMe(currentUser, Map.of("name", "test user"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateMeOnlyCityUpdatesSuccessfullyTest() {
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        Map<String, String> body = new HashMap<>();
        body.put("city", "Madrid");

        ResponseEntity<?> result = userController.updateMe(currentUser, body);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Madrid", currentUser.getCity());
    }

    // @Test
    // void updateMeBlankCitySetsNullTest() {
    //     when(userRepository.save(currentUser)).thenReturn(currentUser);

    //     Map<String, String> body = new HashMap<>();
    //     body.put("city", "   ");

    //     ResponseEntity<?> result = userController.updateMe(currentUser, body);

    //     assertEquals(HttpStatus.OK, result.getStatusCode());
    //     assertNull(currentUser.getCity());
    // }

    // ─── changePassword ───────────────────────────────────────────────────────

    @Test
    void changePasswordNoPasswordHashReturnsBadRequestTest() {
        currentUser.setPasswordHash(null);
        UserController.ChangePasswordRequest req =
                new UserController.ChangePasswordRequest("old", "NewPass1!");

        ResponseEntity<?> result = userController.changePassword(currentUser, req);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("NO_PASSWORD", body.get("code"));
    }

    @Test
    void changePasswordWrongCurrentPasswordReturnsUnauthorizedTest() {
        currentUser.setPasswordHash("$2a$10$hashedpassword");
        when(passwordEncoder.matches("wrongOld", "$2a$10$hashedpassword")).thenReturn(false);

        UserController.ChangePasswordRequest req =
                new UserController.ChangePasswordRequest("wrongOld", "NewPass1!");

        ResponseEntity<?> result = userController.changePassword(currentUser, req);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("WRONG_PASSWORD", body.get("code"));
    }

    @Test
    void changePasswordWeakNewPasswordReturnsBadRequestTest() {
        currentUser.setPasswordHash("$2a$10$hashedpassword");
        when(passwordEncoder.matches("OldPass1!", "$2a$10$hashedpassword")).thenReturn(true);

        UserController.ChangePasswordRequest req =
                new UserController.ChangePasswordRequest("OldPass1!", "weak");

        ResponseEntity<?> result = userController.changePassword(currentUser, req);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("VALIDATION_ERROR", body.get("code"));
    }

    @Test
    void changePasswordSameAsCurrentReturnsBadRequestTest() {
        currentUser.setPasswordHash("$2a$10$hashedpassword");
        when(passwordEncoder.matches("OldPass1!", "$2a$10$hashedpassword")).thenReturn(true);

        UserController.ChangePasswordRequest req =
                new UserController.ChangePasswordRequest("OldPass1!", "OldPass1!");

        ResponseEntity<?> result = userController.changePassword(currentUser, req);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("SAME_PASSWORD", body.get("code"));
    }

    @Test
    void changePasswordSuccessReturnsOkTest() {
        currentUser.setPasswordHash("$2a$10$hashedpassword");
        when(passwordEncoder.matches("OldPass1!", "$2a$10$hashedpassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("$2a$10$newhashedpassword");
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        UserController.ChangePasswordRequest req =
                new UserController.ChangePasswordRequest("OldPass1!", "NewPass1!");

        ResponseEntity<?> result = userController.changePassword(currentUser, req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        verify(userRepository).save(currentUser);
    }

    // ─── deleteMe ─────────────────────────────────────────────────────────────

    @Test
    void deleteMeNoStripeSubscriptionSchedulesDeletionTest() {
        when(stripeService.isConfigured()).thenReturn(false);
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.deleteMe(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(currentUser.getDeletionScheduledAt());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("deletionScheduledAt"));
    }

    @Test
    void deleteMeStripeSubscriptionCancelsAtPeriodEndTest() throws Exception {
        currentUser.setStripeSubscriptionId("sub_test123");
        currentUser.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(true);
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.deleteMe(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(stripeService).cancelAtPeriodEnd("sub_test123");
        assertTrue(currentUser.isSubscriptionCancelAtPeriodEnd());
    }

    @Test
    void deleteMeAlreadyCanceledStripeSkipsCancelTest() throws Exception {
        currentUser.setStripeSubscriptionId("sub_test123");
        currentUser.setSubscriptionStatus("canceled");
        when(stripeService.isConfigured()).thenReturn(true);
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.deleteMe(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(stripeService, never()).cancelAtPeriodEnd(any());
    }

    @Test
    void deleteMeSubscriptionPeriodEndExtendsScheduledDeletionTest() {
        LocalDateTime farFuture = LocalDateTime.now().plusDays(60);
        currentUser.setSubscriptionCurrentPeriodEnd(farFuture);
        when(stripeService.isConfigured()).thenReturn(false);
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.deleteMe(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        // Deletion date should match the subscription period end
        assertEquals(farFuture, currentUser.getDeletionScheduledAt());
    }

    @Test
    void deleteMeStripeCancelFailsStillSchedulesDeletionTest() throws Exception {
        currentUser.setStripeSubscriptionId("sub_failing");
        currentUser.setSubscriptionStatus("active");
        when(stripeService.isConfigured()).thenReturn(true);
        doThrow(new RuntimeException("Stripe error")).when(stripeService).cancelAtPeriodEnd("sub_failing");
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.deleteMe(currentUser);

        // Should still succeed despite the Stripe failure
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(currentUser.getDeletionScheduledAt());
    }

    // ─── getPublicProfile ─────────────────────────────────────────────────────

    @Test
    void getPublicProfileFoundReturnsOkTest() {
        UUID userId = UUID.randomUUID();
        User profileUser = new User();
        profileUser.setId(userId);
        profileUser.setName("Public User");
        profileUser.setEmail("public@test.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(profileUser));
        when(pdfDocumentRepository.countByOwnerId(userId)).thenReturn(3L);

        ResponseEntity<?> result = userController.getPublicProfile(userId, currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    void getPublicProfileNotFoundReturnsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        ResponseEntity<?> result = userController.getPublicProfile(unknownId, currentUser);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    // ─── reportUser ───────────────────────────────────────────────────────────

    @Test
    void reportUserNullCurrentUserReturnsUnauthorizedTest() {
        UUID targetId = UUID.randomUUID();

        ResponseEntity<?> result = userController.reportUser(targetId, Map.of("reason", "spam"), null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void reportUserTargetNotFoundReturnsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        ResponseEntity<?> result = userController.reportUser(unknownId, Map.of("reason", "spam"), currentUser);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void reportUserSuccessReturnsOkTest() {
        UUID targetId = UUID.randomUUID();
        User reported = new User();
        reported.setId(targetId);
        reported.setName("Bad Actor");
        reported.setEmail("bad@test.com");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(reported));
        when(userReportRepository.save(any(UserReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> result = userController.reportUser(targetId, Map.of("reason", "spam"), currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userReportRepository).save(any(UserReport.class));
    }

    // ─── blockUser ────────────────────────────────────────────────────────────

    @Test
    void blockUserNullCurrentUserReturnsUnauthorizedTest() {
        UUID targetId = UUID.randomUUID();

        ResponseEntity<?> result = userController.blockUser(targetId, null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void blockUserTargetNotFoundReturnsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        ResponseEntity<?> result = userController.blockUser(unknownId, currentUser);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void blockUserSuccessSavesBlockTest() {
        UUID targetId = UUID.randomUUID();
        User blocked = new User();
        blocked.setId(targetId);
        blocked.setName("Blocked User");
        blocked.setEmail("blocked@test.com");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(blocked));
        when(userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), targetId)).thenReturn(false);
        when(userBlockRepository.save(any(UserBlock.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> result = userController.blockUser(targetId, currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userBlockRepository).save(any(UserBlock.class));
    }

    @Test
    void blockUserAlreadyBlockedDoesNotSaveDuplicateTest() {
        UUID targetId = UUID.randomUUID();
        User blocked = new User();
        blocked.setId(targetId);
        blocked.setName("Already Blocked");
        blocked.setEmail("blocked@test.com");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(blocked));
        when(userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), targetId)).thenReturn(true);

        ResponseEntity<?> result = userController.blockUser(targetId, currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userBlockRepository, never()).save(any());
    }

    // ─── unblockUser ──────────────────────────────────────────────────────────

    @Test
    void unblockUserNullCurrentUserReturnsUnauthorizedTest() {
        UUID targetId = UUID.randomUUID();

        ResponseEntity<?> result = userController.unblockUser(targetId, null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void unblockUserExistingBlockDeletesItTest() {
        UUID targetId = UUID.randomUUID();
        UserBlock block = UserBlock.builder()
                .blocker(currentUser)
                .build();

        when(userBlockRepository.findByBlockerIdAndBlockedId(currentUser.getId(), targetId))
                .thenReturn(Optional.of(block));

        ResponseEntity<?> result = userController.unblockUser(targetId, currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userBlockRepository).delete(block);
    }

    @Test
    void unblockUserNoExistingBlockReturnsOkTest() {
        UUID targetId = UUID.randomUUID();
        when(userBlockRepository.findByBlockerIdAndBlockedId(currentUser.getId(), targetId))
                .thenReturn(Optional.empty());

        ResponseEntity<?> result = userController.unblockUser(targetId, currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userBlockRepository, never()).delete(any());
    }

    // ─── toggleAcceptsMessages ────────────────────────────────────────────────

    @Test
    void toggleAcceptsMessagesNullCurrentUserReturnsUnauthorizedTest() {
        ResponseEntity<?> result = userController.toggleAcceptsMessages(Map.of("acceptsMessages", true), null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void toggleAcceptsMessagesMissingFieldReturnsBadRequestTest() {
        Map<String, Boolean> body = new HashMap<>();
        body.put("acceptsMessages", null);

        ResponseEntity<?> result = userController.toggleAcceptsMessages(body, currentUser);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void toggleAcceptsMessagesTrueUpdatesUserTest() {
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.toggleAcceptsMessages(Map.of("acceptsMessages", true), currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(currentUser.isAcceptsMessages());
        verify(userRepository).save(currentUser);
    }

    @Test
    void toggleAcceptsMessagesFalseUpdatesUserTest() {
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.toggleAcceptsMessages(Map.of("acceptsMessages", false), currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(currentUser.isAcceptsMessages());
        verify(userRepository).save(currentUser);
    }

    // ─── cancelDeletion ───────────────────────────────────────────────────────

    @Test
    void cancelDeletionNoDeletionScheduledReturnsBadRequestTest() {
        currentUser.setDeletionScheduledAt(null);

        ResponseEntity<?> result = userController.cancelDeletion(currentUser);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void cancelDeletionClearsDateAndSavesTest() {
        currentUser.setDeletionScheduledAt(LocalDateTime.now().plusDays(10));
        when(stripeService.isConfigured()).thenReturn(false);
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.cancelDeletion(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNull(currentUser.getDeletionScheduledAt());
        verify(userRepository).save(currentUser);
    }

    @Test
    void cancelDeletionReactivatesStripeSubscriptionTest() throws Exception {
        currentUser.setDeletionScheduledAt(LocalDateTime.now().plusDays(10));
        currentUser.setStripeSubscriptionId("sub_reactivate123");
        currentUser.setSubscriptionCancelAtPeriodEnd(true);
        when(stripeService.isConfigured()).thenReturn(true);
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.cancelDeletion(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(stripeService).reactivateSubscription("sub_reactivate123");
        assertFalse(currentUser.isSubscriptionCancelAtPeriodEnd());
    }

    @Test
    void cancelDeletionStripeReactivationFailsStillClearsDateTest() throws Exception {
        currentUser.setDeletionScheduledAt(LocalDateTime.now().plusDays(10));
        currentUser.setStripeSubscriptionId("sub_fail");
        currentUser.setSubscriptionCancelAtPeriodEnd(true);
        when(stripeService.isConfigured()).thenReturn(true);
        doThrow(new RuntimeException("Stripe error")).when(stripeService).reactivateSubscription("sub_fail");
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        ResponseEntity<?> result = userController.cancelDeletion(currentUser);

        // Should still succeed and clear the deletion date
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNull(currentUser.getDeletionScheduledAt());
    }

    // ─── checkNameForUpdate ───────────────────────────────────────────────────

    @Test
    void checkNameForUpdateNameTakenByOtherReturnsTrue() {
        when(userRepository.existsByNameIgnoreCase("OtherUser")).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> result = userController.checkNameForUpdate("OtherUser", currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().get("taken"));
    }

    @Test
    void checkNameForUpdateSameNameReturnsFalseTest() {
        // currentUser name is "Test User" — same name returns not taken
        when(userRepository.existsByNameIgnoreCase("Test User")).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> result = userController.checkNameForUpdate("Test User", currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().get("taken"));
    }

    @Test
    void checkNameForUpdateFreeNameReturnsFalseTest() {
        when(userRepository.existsByNameIgnoreCase("FreeUser")).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> result = userController.checkNameForUpdate("FreeUser", currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().get("taken"));
    }

    // ─── getUserTabs ──────────────────────────────────────────────────────────

    @Test
    void getUserTabsReturnsOkWithListTest() {
        UUID userId = UUID.randomUUID();
        when(pdfStorageService.getPublicTabsByUser(userId, currentUser)).thenReturn(List.of());

        ResponseEntity<?> result = userController.getUserTabs(userId, currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(pdfStorageService).getPublicTabsByUser(userId, currentUser);
    }
}
