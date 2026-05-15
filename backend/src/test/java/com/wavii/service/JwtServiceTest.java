package com.wavii.service;

import com.wavii.model.User;
import com.wavii.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry", 86400L); // 1 day
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiry", 604800L); // 7 days
    }

    @Test
    void generateAccessTokenTest() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRole(Role.USUARIO);

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertEquals("test@test.com", jwtService.extractEmail(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void generateRefreshTokenTest() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRole(Role.USUARIO);

        String token = jwtService.generateRefreshToken(user);

        assertNotNull(token);
        assertEquals("test@test.com", jwtService.extractEmail(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValidInvalidEmailTest() {
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setEmail("test1@test.com");
        user1.setRole(Role.USUARIO);

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setEmail("test2@test.com");

        String token = jwtService.generateAccessToken(user1);

        assertFalse(jwtService.isTokenValid(token, user2));
    }

    @Test
    void isTokenExpiredWithExpiredTokenTest() {
        JwtService shortLivedJwtService = new JwtService();
        ReflectionTestUtils.setField(shortLivedJwtService, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(shortLivedJwtService, "accessTokenExpiry", -100L); // Negative expiry
        ReflectionTestUtils.setField(shortLivedJwtService, "refreshTokenExpiry", -100L);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRole(Role.USUARIO);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            String token = shortLivedJwtService.generateAccessToken(user);
            shortLivedJwtService.isTokenExpired(token);
        });
    }
}
