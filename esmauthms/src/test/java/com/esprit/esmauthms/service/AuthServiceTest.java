package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.*;
import com.esprit.esmauthms.entity.User;
import com.esprit.esmauthms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — registration and login flows")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock EmailClient emailClient;

    @InjectMocks AuthService authService;

    private User verifiedActiveUser;

    @BeforeEach
    void setUp() {
        verifiedActiveUser = User.builder()
                .id(UUID.randomUUID())
                .email("student@esm.com")
                .password("$2a$10$encoded")
                .role("STUDENT")
                .status("ACTIVE")
                .isEmailVerified(true)
                .twoFactorEnabled(false)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — new email → user saved and token returned")
    void register_newEmail_savesUserAndReturnsToken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@esm.com");
        req.setPassword("pass123");
        req.setFirstName("Alice");
        req.setRole("STUDENT");

        when(userRepository.findByEmail("new@esm.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn("jwt-token");
        doNothing().when(emailClient).sendEmail(anyString(), anyString(), anyString());

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register — duplicate email → throws RuntimeException")
    void register_duplicateEmail_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("student@esm.com");
        req.setPassword("pass123");

        when(userRepository.findByEmail("student@esm.com"))
                .thenReturn(Optional.of(verifiedActiveUser));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login — verified active user → token in response")
    void login_verifiedActiveUser_returnsToken() {
        when(userRepository.findByEmail("student@esm.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(passwordEncoder.matches("password", "$2a$10$encoded")).thenReturn(true);
        when(jwtService.generateToken(any(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn("final-jwt");
        when(userRepository.save(any())).thenReturn(verifiedActiveUser);

        AuthFlowResponse response = authService.login(new AuthRequest("student@esm.com", "password"));

        assertThat(response.getAccessToken()).isEqualTo("final-jwt");
        assertThat(response.isEmailVerified()).isTrue();
        assertThat(response.isTwoFactorRequired()).isFalse();
    }

    @Test
    @DisplayName("login — wrong password → throws RuntimeException")
    void login_wrongPassword_throwsException() {
        when(userRepository.findByEmail("student@esm.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new AuthRequest("student@esm.com", "wrong")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("login — email not verified → no token, emailVerified=false")
    void login_unverifiedEmail_returnsNoToken() {
        User unverified = User.builder()
                .id(UUID.randomUUID())
                .email("unverified@esm.com")
                .password("$2a$10$encoded")
                .role("STUDENT")
                .status("ACTIVE")
                .isEmailVerified(false)
                .twoFactorEnabled(false)
                .emailVerificationToken("tok")
                .emailVerificationExpiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(userRepository.findByEmail("unverified@esm.com"))
                .thenReturn(Optional.of(unverified));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        doNothing().when(emailClient).sendEmail(anyString(), anyString(), anyString());

        AuthFlowResponse response = authService.login(new AuthRequest("unverified@esm.com", "pass"));

        assertThat(response.getAccessToken()).isNull();
        assertThat(response.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("login — account suspended → no token, status returned")
    void login_suspendedAccount_returnsNoToken() {
        User suspended = User.builder()
                .id(UUID.randomUUID())
                .email("suspended@esm.com")
                .password("$2a$10$encoded")
                .role("STUDENT")
                .status("SUSPENDED")
                .isEmailVerified(true)
                .twoFactorEnabled(false)
                .build();

        when(userRepository.findByEmail("suspended@esm.com"))
                .thenReturn(Optional.of(suspended));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        AuthFlowResponse response = authService.login(new AuthRequest("suspended@esm.com", "pass"));

        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getAccountStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("login — 2FA enabled → twoFactorRequired=true, code sent by email")
    void login_twoFactorEnabled_returnsTwoFactorRequired() {
        User twoFaUser = User.builder()
                .id(UUID.randomUUID())
                .email("2fa@esm.com")
                .password("$2a$10$encoded")
                .role("TUTOR")
                .status("ACTIVE")
                .isEmailVerified(true)
                .twoFactorEnabled(true)
                .build();

        when(userRepository.findByEmail("2fa@esm.com")).thenReturn(Optional.of(twoFaUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.save(any())).thenReturn(twoFaUser);
        doNothing().when(emailClient).sendEmail(anyString(), anyString(), anyString());

        AuthFlowResponse response = authService.login(new AuthRequest("2fa@esm.com", "pass"));

        assertThat(response.getAccessToken()).isNull();
        assertThat(response.isTwoFactorRequired()).isTrue();
        verify(emailClient).sendEmail(eq("2fa@esm.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("login — user not found → throws RuntimeException")
    void login_userNotFound_throwsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthRequest("ghost@esm.com", "pass")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
