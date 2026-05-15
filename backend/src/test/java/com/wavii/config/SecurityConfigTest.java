package com.wavii.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void authenticationProviderBeanCreatedTest() {
        AuthenticationProvider provider = securityConfig.authenticationProvider();
        assertNotNull(provider);
    }

    @Test
    void authenticationManagerBeanCreatedTest() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(manager);

        AuthenticationManager result = securityConfig.authenticationManager(config);
        assertNotNull(result);
    }

    @Test
    void passwordEncoderBeanCreatedTest() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
    }

    @SuppressWarnings("unchecked")
    @Test
    void securityFilterChainConfiguredTest() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        doReturn(chain).when(http).build();

        doAnswer(inv -> {
            Customizer<CsrfConfigurer<HttpSecurity>> c = inv.getArgument(0);
            c.customize(mock(CsrfConfigurer.class, RETURNS_DEEP_STUBS));
            return http;
        }).when(http).csrf(any());

        doAnswer(inv -> {
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> c =
                    inv.getArgument(0);
            c.customize(mock(
                    AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class,
                    RETURNS_DEEP_STUBS));
            return http;
        }).when(http).authorizeHttpRequests(any());

        doAnswer(inv -> {
            Customizer<SessionManagementConfigurer<HttpSecurity>> c = inv.getArgument(0);
            c.customize(mock(SessionManagementConfigurer.class, RETURNS_DEEP_STUBS));
            return http;
        }).when(http).sessionManagement(any());

        doAnswer(inv -> {
            Customizer<ExceptionHandlingConfigurer<HttpSecurity>> c = inv.getArgument(0);
            c.customize(mock(ExceptionHandlingConfigurer.class, RETURNS_DEEP_STUBS));
            return http;
        }).when(http).exceptionHandling(any());

        doReturn(http).when(http).authenticationProvider(any());
        doReturn(http).when(http).addFilterBefore(any(), eq(UsernamePasswordAuthenticationFilter.class));

        SecurityFilterChain result = securityConfig.securityFilterChain(http);

        assertNotNull(result);
    }

    @Test
    void webSecurityCustomizerBeanCreatedTest() {
        var customizer = securityConfig.webSecurityCustomizer();
        assertNotNull(customizer);
    }
}
