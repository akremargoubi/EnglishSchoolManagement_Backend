// src/main/java/com/esprit/esmauthms/service/JwtService.java
package com.esprit.esmauthms.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Existing method (kept)
    public String generateToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key)
                .compact();
    }

    // Extended method
    public String generateToken(UUID userId,
                                String role,
                                String status,
                                boolean emailVerified,
                                boolean twoFactorEnabled) {
        return generateToken(userId, null, role, status, emailVerified, twoFactorEnabled);
    }

    public String generateToken(UUID userId,
                                String email,
                                String role,
                                String status,
                                boolean emailVerified,
                                boolean twoFactorEnabled) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("status", status)
                .claim("emailVerified", emailVerified)
                .claim("twoFactorEnabled", twoFactorEnabled)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractStatus(String token) {
        return parseClaims(token).get("status", String.class);
    }

    public boolean extractEmailVerified(String token) {
        Boolean value = parseClaims(token).get("emailVerified", Boolean.class);
        return value != null && value;
    }

    public boolean extractTwoFactorEnabled(String token) {
        Boolean value = parseClaims(token).get("twoFactorEnabled", Boolean.class);
        return value != null && value;
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
