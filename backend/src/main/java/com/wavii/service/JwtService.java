package com.wavii.service;

import com.wavii.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${wavii.jwt.secret}")
    private String secret;

    @Value("${wavii.jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${wavii.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    // ---- Public API ----

    /**
     * Genera un token de acceso (JWT) para el usuario.
     * 
     * @param user Usuario para el que se genera el token.
     * @return Token de acceso firmado.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().name());
        return buildToken(claims, user.getEmail(), accessTokenExpiry * 1000L);
    }

    /**
     * Genera un token de refresco (JWT) para el usuario.
     * 
     * @param user Usuario para el que se genera el token.
     * @return Token de refresco firmado.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");
        return buildToken(claims, user.getEmail(), refreshTokenExpiry * 1000L);
    }

    /**
     * Extrae el email del usuario (subject) contenido en el token.
     * 
     * @param token Token JWT.
     * @return Email del usuario.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Valida si un token pertenece al usuario y no ha expirado.
     * 
     * @param token Token JWT.
     * @param user Usuario contra el que validar.
     * @return true si el token es válido.
     */
    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }

    /**
     * Comprueba si un token ha superado su fecha de expiración.
     * 
     * @param token Token JWT.
     * @return true si ha expirado.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ---- Private helpers ----

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
