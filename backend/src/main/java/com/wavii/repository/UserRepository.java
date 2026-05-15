package com.wavii.repository;

import com.wavii.model.User;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNameIgnoreCase(String name);

    Optional<User> findByNameIgnoreCase(String name);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    List<User> findByRoleIn(Collection<Role> roles);

    List<User> findBySubscriptionCancelAtPeriodEndTrueAndSubscriptionCurrentPeriodEndBeforeAndSubscriptionNot(
            LocalDateTime now,
            Subscription subscription
    );
}
