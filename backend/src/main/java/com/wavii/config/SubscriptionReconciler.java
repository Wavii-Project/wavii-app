package com.wavii.config;

import com.wavii.model.User;
import com.wavii.model.enums.Subscription;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reconciliador de suscripciones canceladas.
 * Tarea programada que revisa periódicamente los usuarios cuyas suscripciones han expirado
 * tras una cancelación y los devuelve al plan FREE.
 * 
 * @author eduglezexp
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionReconciler {

    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 60 * 60 * 1000L)
    @Transactional
    public void reconcileCancelledSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<User> users = userRepository
                .findBySubscriptionCancelAtPeriodEndTrueAndSubscriptionCurrentPeriodEndBeforeAndSubscriptionNot(
                        now,
                        Subscription.FREE);

        for (User user : users) {
            user.setSubscription(Subscription.FREE);
            user.setSubscriptionStatus("canceled");
            user.setSubscriptionCancelAtPeriodEnd(false);
            user.setStripeSubscriptionId(null);
            userRepository.save(user);
            log.info("Suscripcion pasada a FREE por fin de periodo para {}", user.getEmail());
        }
    }
}
