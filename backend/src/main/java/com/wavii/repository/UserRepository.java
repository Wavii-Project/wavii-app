package com.wavii.repository;

import com.wavii.model.User;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la entidad User.
 * Gestiona el acceso a datos de los usuarios de la plataforma.
 * 
 * @author eduglezexp
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /** Busca un usuario por su dirección de email. */
    Optional<User> findByEmail(String email);

    /** Comprueba si ya existe un usuario con el email indicado. */
    boolean existsByEmail(String email);

    /** Comprueba si ya existe un usuario con el nombre indicado (ignorando mayúsculas). */
    boolean existsByNameIgnoreCase(String name);

    /** Busca un usuario por su nombre (ignorando mayúsculas). */
    Optional<User> findByNameIgnoreCase(String name);

    /** Busca un usuario por su ID de cliente de Stripe. */
    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    /** Obtiene una lista de usuarios que tengan alguno de los roles indicados. */
    List<User> findByRoleIn(Collection<Role> roles);

    /** Busca usuarios cuya suscripción haya expirado y esté marcada para no renovar. */
    List<User> findBySubscriptionCancelAtPeriodEndTrueAndSubscriptionCurrentPeriodEndBeforeAndSubscriptionNot(
            LocalDateTime now,
            Subscription subscription
    );
}
