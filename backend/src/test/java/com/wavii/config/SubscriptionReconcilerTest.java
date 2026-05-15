package com.wavii.config;

import com.wavii.model.User;
import com.wavii.model.enums.Subscription;
import com.wavii.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionReconcilerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionReconciler subscriptionReconciler;

    private User plusUser;

    @BeforeEach
    void setUp() {
        plusUser = new User();
        plusUser.setId(UUID.randomUUID());
        plusUser.setEmail("plus@wavii.app");
        plusUser.setSubscription(Subscription.PLUS);
        plusUser.setSubscriptionCancelAtPeriodEnd(true);
        plusUser.setSubscriptionCurrentPeriodEnd(LocalDateTime.now().minusHours(1));
        plusUser.setStripeSubscriptionId("sub_test123");
    }

    @Test
    void reconcileCancelledSubscriptionsDowngradesExpiredUsersToFreeTest() {
        when(userRepository.findBySubscriptionCancelAtPeriodEndTrueAndSubscriptionCurrentPeriodEndBeforeAndSubscriptionNot(
                any(LocalDateTime.class), eq(Subscription.FREE))).thenReturn(List.of(plusUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionReconciler.reconcileCancelledSubscriptions();

        assertEquals(Subscription.FREE, plusUser.getSubscription());
        assertEquals("canceled", plusUser.getSubscriptionStatus());
        assertFalse(plusUser.isSubscriptionCancelAtPeriodEnd());
        assertNull(plusUser.getStripeSubscriptionId());
        verify(userRepository).save(plusUser);
    }

    @Test
    void reconcileCancelledSubscriptionsNoExpiredUsersDoesNothingTest() {
        when(userRepository.findBySubscriptionCancelAtPeriodEndTrueAndSubscriptionCurrentPeriodEndBeforeAndSubscriptionNot(
                any(LocalDateTime.class), eq(Subscription.FREE))).thenReturn(List.of());

        subscriptionReconciler.reconcileCancelledSubscriptions();

        verify(userRepository, never()).save(any());
    }

    @Test
    void reconcileCancelledSubscriptionsMultipleUsersAllDowngradedTest() {
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setEmail("user1@wavii.app");
        user1.setSubscription(Subscription.PLUS);
        user1.setSubscriptionCancelAtPeriodEnd(true);
        user1.setStripeSubscriptionId("sub_1");

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setEmail("user2@wavii.app");
        user2.setSubscription(Subscription.PLUS);
        user2.setSubscriptionCancelAtPeriodEnd(true);
        user2.setStripeSubscriptionId("sub_2");

        when(userRepository.findBySubscriptionCancelAtPeriodEndTrueAndSubscriptionCurrentPeriodEndBeforeAndSubscriptionNot(
                any(LocalDateTime.class), eq(Subscription.FREE))).thenReturn(List.of(user1, user2));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionReconciler.reconcileCancelledSubscriptions();

        assertEquals(Subscription.FREE, user1.getSubscription());
        assertEquals(Subscription.FREE, user2.getSubscription());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void reconcileCancelledSubscriptionsSetsCorrectStatusFieldsTest() {
        when(userRepository.findBySubscriptionCancelAtPeriodEndTrueAndSubscriptionCurrentPeriodEndBeforeAndSubscriptionNot(
                any(LocalDateTime.class), eq(Subscription.FREE))).thenReturn(List.of(plusUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionReconciler.reconcileCancelledSubscriptions();

        assertNull(plusUser.getStripeSubscriptionId());
        assertEquals("canceled", plusUser.getSubscriptionStatus());
        assertFalse(plusUser.isSubscriptionCancelAtPeriodEnd());
    }
}
