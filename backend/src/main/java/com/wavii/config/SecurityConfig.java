package com.wavii.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuracion de seguridad principal para la aplicacion Wavii.
 * Define las reglas de acceso, filtros JWT y la gestion de sesiones.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final @Lazy JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * Cadena de seguridad especifica para Actuator.
     * Mantiene publico el healthcheck usado por Docker y diagnostico externo.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }

    /**
     * Configura la cadena de filtros principal de la API.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/auth/test-email",
                                "/api/auth/check-verification",
                                "/api/auth/check-name",
                                "/api/auth/verify-teacher-phone",
                                "/api/auth/confirm-teacher-phone",
                                "/api/subscription/webhook",
                                "/api/verification/odoo-webhook",
                                "/ws/**",
                                "/uploads/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pdfs/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pdfs/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pdfs/*/download").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bands/my", "/api/band-listings/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/bands", "/api/bands/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/band-listings", "/api/band-listings/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/news").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/tabs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/forums/my", "/api/forums", "/api/forums/*").authenticated()
                        .requestMatchers("/api/onboarding/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/verification/approve/**").hasRole("ADMIN")
                        .requestMatchers("/api/verification/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Define el proveedor de autenticacion que utiliza el servicio de usuarios personalizado y BCrypt.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Bean para gestionar la autenticacion en los controladores o servicios.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Bean para el cifrado de contrasenas mediante BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Personalizacion para ignorar la seguridad en rutas especificas.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                new AntPathRequestMatcher("/api/subscription/webhook")
        );
    }
}
