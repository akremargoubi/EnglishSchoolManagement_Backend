package com.esprit.esmauthms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService — token generation and validation")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "testsecretkeytestsecretkeytestsecretkey12345");
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 3_600_000L);
        jwtService.init();
    }

    // ── generation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken — subject equals userId")
    void generateToken_subjectEqualsUserId() {
        UUID id = UUID.randomUUID();
        String token = jwtService.generateToken(id);
        assertThat(jwtService.extractUserId(token)).isEqualTo(id);
    }

    @Test
    @DisplayName("generateToken (full) — all claims are present")
    void generateToken_fullClaims() {
        UUID id = UUID.randomUUID();
        String token = jwtService.generateToken(
                id, "user@test.com", "TUTOR", "ACTIVE", true, false);

        assertThat(jwtService.extractUserId(token)).isEqualTo(id);
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("TUTOR");
        assertThat(jwtService.extractStatus(token)).isEqualTo("ACTIVE");
        assertThat(jwtService.extractEmailVerified(token)).isTrue();
        assertThat(jwtService.extractTwoFactorEnabled(token)).isFalse();
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValid — valid token returns true")
    void isValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(UUID.randomUUID());
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid — tampered token returns false")
    void isValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid — blank string returns false")
    void isValid_blankToken_returnsFalse() {
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid — expired token returns false")
    void isValid_expiredToken_returnsFalse() {
        // create a service instance with 0ms expiration
        JwtService expiredSvc = new JwtService();
        ReflectionTestUtils.setField(expiredSvc, "secret",
                "testsecretkeytestsecretkeytestsecretkey12345");
        ReflectionTestUtils.setField(expiredSvc, "accessExpiration", 1L);
        expiredSvc.init();

        String token = expiredSvc.generateToken(UUID.randomUUID());
        assertThat(expiredSvc.isValid(token)).isFalse();
    }

    // ── extraction ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractRole — returns correct role")
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateToken(
                UUID.randomUUID(), null, "ADMIN", "ACTIVE", true, false);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractTwoFactorEnabled — returns true when set")
    void extractTwoFactorEnabled_returnsTrue() {
        String token = jwtService.generateToken(
                UUID.randomUUID(), null, "STUDENT", "ACTIVE", true, true);
        assertThat(jwtService.extractTwoFactorEnabled(token)).isTrue();
    }
}
