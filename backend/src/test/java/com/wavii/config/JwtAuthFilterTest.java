package com.wavii.config;

import com.wavii.model.User;
import com.wavii.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalNoAuthHeaderTest() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternalValidTokenTest() throws Exception {
        String token = "valid_token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn("test@test.com");

        User user = new User();
        user.setEmail("test@test.com");
        when(customUserDetailsService.loadUserByUsername("test@test.com")).thenReturn(user);
        when(jwtService.isTokenValid(token, user)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void doFilterInternalInvalidTokenTest() throws Exception {
        String token = "invalid_token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenThrow(new RuntimeException("Invalid token"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }
}
