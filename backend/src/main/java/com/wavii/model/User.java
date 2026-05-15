package com.wavii.model;

import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa a un usuario en el sistema.
 * Implementa UserDetails para la integración con Spring Security.
 * 
 * @author eduglezexp
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Si true, el usuario acepta mensajes directos de otros usuarios */
    @Column(nullable = false)
    @Builder.Default
    private boolean acceptsMessages = true;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USUARIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Subscription subscription = Subscription.FREE;

    @Enumerated(EnumType.STRING)
    private Level level;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean teacherVerified = false;

    private String googleId;

    private String avatarUrl;

    private String stripeCustomerId;

    private String stripeSubscriptionId;

    private String subscriptionStatus;

    /**
     * true cuando el usuario ya ha usado (o consumido) el periodo de prueba de Plus
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean trialUsed = false;

    /**
     * true cuando la suscripción está programada para cancelarse al final del
     * periodo
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean subscriptionCancelAtPeriodEnd = false;

    /**
     * fecha en que termina el ciclo de facturación actual (sincronizada con Stripe)
     */
    private LocalDateTime subscriptionCurrentPeriodEnd;

    /**
     * si está fijado, la cuenta quedará eliminada en esta fecha (usuario puede
     * cancelarlo)
     */
    private LocalDateTime deletionScheduledAt;

    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String instrument;

    @Column(length = 120)
    private String city;

    /** Latitud GPS (opcional, para «Profesores cerca de ti») */
    private Double latitude;

    /** Longitud GPS (opcional) */
    private Double longitude;

    /** Dirección completa del lugar donde imparte clases presenciales (opcional) */
    @Column(length = 250)
    private String address;

    @Column(length = 120)
    private String province;

    @Column(length = 160)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 300)
    private String instagramUrl;

    @Column(length = 300)
    private String tiktokUrl;

    @Column(length = 300)
    private String youtubeUrl;

    @Column(length = 300)
    private String facebookUrl;

    @Column(length = 300)
    private String bannerImageUrl;

    /**
     * Preferencia de disponibilidad: MORNING | AFTERNOON | ANYTIME | CUSTOM
     */
    @Column(length = 20)
    private String availabilityPreference;

    @Column(length = 500)
    private String availabilityNotes;

    @ElementCollection
    @CollectionTable(name = "user_place_image_urls", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "image_url", length = 300)
    @Builder.Default
    private List<String> placeImageUrls = new ArrayList<>();

    /**
     * Modalidad de clases: PRESENCIAL | ONLINE | AMBAS
     * Almacenado como string para no requerir una migración de enum complicada.
     */
    @Column(length = 20)
    private String classModality;

    @Column(name = "price_per_hour", precision = 10, scale = 2)
    private java.math.BigDecimal pricePerHour;

    @Builder.Default
    private int xp = 0;

    @Builder.Default
    private int streak = 0;

    /** Mejor racha historica del usuario */
    @Builder.Default
    private int bestStreak = 0;

    /**
     * Ultimo dia en que el usuario completo al menos un desafio (para calcular la
     * racha)
     */
    private LocalDate lastStreakDate;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLoginAt;

    // ----- UserDetails -----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
