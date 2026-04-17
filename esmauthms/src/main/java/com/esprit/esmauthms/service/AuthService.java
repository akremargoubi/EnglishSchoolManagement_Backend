// src/main/java/com/esprit/esmauthms/service/AuthService.java
package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.*;
import com.esprit.esmauthms.entity.User;
import com.esprit.esmauthms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailClient emailClient;

    // ===================== REGISTRATION =====================

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                // ✅ FIXED: persist all profile fields so users appear in search
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .cin(request.getCin())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(request.getRole() != null ? request.getRole() : "USER")
                .status("ACTIVE")
                .isEmailVerified(false)
                .twoFactorEnabled(false)
                .build();

        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusDays(1));

        userRepository.save(user);

        sendVerificationEmail(user);

        String token = jwtService.generateToken(
                user.getId(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.isTwoFactorEnabled()
        );

        return new AuthResponse(token);
    }

    // ===================== LOGIN (MULTI-STEP) =====================

    public AuthFlowResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Step 1: email verification gate
        if (!user.isEmailVerified()) {
            if (user.getEmailVerificationToken() == null
                    || user.getEmailVerificationExpiresAt() == null
                    || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
                user.setEmailVerificationToken(UUID.randomUUID().toString());
                user.setEmailVerificationExpiresAt(LocalDateTime.now().plusDays(1));
                userRepository.save(user);
            }

            sendVerificationEmail(user);

            return AuthFlowResponse.builder()
                    .accessToken(null)
                    .emailVerified(false)
                    .accountStatus(user.getStatus())
                    .twoFactorRequired(false)
                    .maskedEmail(maskEmail(user.getEmail()))
                    .build();
        }

        // Step 2: account status gate
        if (user.getStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return AuthFlowResponse.builder()
                    .accessToken(null)
                    .emailVerified(true)
                    .accountStatus(user.getStatus())
                    .twoFactorRequired(false)
                    .maskedEmail(maskEmail(user.getEmail()))
                    .build();
        }

        // Step 3: 2FA gate
        if (user.isTwoFactorEnabled()) {
            String code = generateSixDigitCode();
            user.setTwoFactorCode(code);
            user.setTwoFactorCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            String subject = "Your 2FA code - ESM Platform";
            String text = "Your verification code is: " + code + "\nThis code expires in 10 minutes.";
            emailClient.sendEmail(user.getEmail(), subject, text);

            return AuthFlowResponse.builder()
                    .accessToken(null)
                    .emailVerified(true)
                    .accountStatus(user.getStatus())
                    .twoFactorRequired(true)
                    .maskedEmail(maskEmail(user.getEmail()))
                    .build();
        }

        // No 2FA => issue final token
        String token = jwtService.generateToken(
                user.getId(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.isTwoFactorEnabled()
        );

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return AuthFlowResponse.builder()
                .accessToken(token)
                .emailVerified(true)
                .accountStatus(user.getStatus())
                .twoFactorRequired(false)
                .maskedEmail(maskEmail(user.getEmail()))
                .build();
    }

    // ===================== 2FA VERIFY =====================

    public AuthFlowResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isTwoFactorEnabled()) {
            throw new RuntimeException("2FA is not enabled for this user");
        }

        if (user.getTwoFactorCode() == null || user.getTwoFactorCodeExpiresAt() == null) {
            throw new RuntimeException("No 2FA code requested");
        }

        if (user.getTwoFactorCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("2FA code has expired");
        }

        if (!user.getTwoFactorCode().equals(request.getCode())) {
            throw new RuntimeException("Invalid 2FA code");
        }

        user.setTwoFactorCode(null);
        user.setTwoFactorCodeExpiresAt(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getId(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.isTwoFactorEnabled()
        );

        return AuthFlowResponse.builder()
                .accessToken(token)
                .emailVerified(true)
                .accountStatus(user.getStatus())
                .twoFactorRequired(false)
                .maskedEmail(maskEmail(user.getEmail()))
                .build();
    }

    // ===================== 2FA ENABLE / DISABLE =====================

    public void enableTwoFactor(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Verify your email before enabling 2FA");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    public void disableTwoFactor(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTwoFactorEnabled(false);
        user.setTwoFactorCode(null);
        user.setTwoFactorCodeExpiresAt(null);
        userRepository.save(user);
    }

    // ===================== EMAIL VERIFICATION RESEND =====================

    public void sendEmailVerificationLink(EmailVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            return;
        }

        if (user.getEmailVerificationToken() == null
                || user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            user.setEmailVerificationToken(UUID.randomUUID().toString());
            user.setEmailVerificationExpiresAt(LocalDateTime.now().plusDays(1));
            userRepository.save(user);
        }

        sendVerificationEmail(user);
    }

    // ===================== EMAIL VERIFICATION (CONFIRM) =====================

    public void verifyEmail(EmailVerifyRequest request) {
        String token = request.getToken();

        User user = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getEmailVerificationToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid email verification token"));

        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Email verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    // ===================== PASSWORD RESET =====================

    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        String subject = "Reset your password - ESM Platform";
        String resetLink = "http://localhost:4200/reset-password?token=" + token;

        String text = "Hello,\n\nWe received a request to reset your password.\n"
                + "You can reset it by opening the following link:\n\n"
                + resetLink + "\n\n"
                + "This link/code is valid for 30 minutes.";

        emailClient.sendEmail(user.getEmail(), subject, text);
    }

    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> request.getToken().equals(u.getPasswordResetToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        if (user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Password reset token has expired");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }

    // ===================== HELPERS =====================

    private void sendVerificationEmail(User user) {
        String subject = "Verify your email - ESM Platform";
        String verifyLink = "http://localhost:4200/verify-email?token=" + user.getEmailVerificationToken();

        String text = "Hello,\n\nPlease verify your email address to access the platform.\n\n"
                + "Click this link to verify:\n"
                + verifyLink + "\n\n"
                + "This link is valid for 24 hours.";

        emailClient.sendEmail(user.getEmail(), subject, text);
    }

    private String generateSixDigitCode() {
        Random random = new Random();
        int num = 100_000 + random.nextInt(900_000);
        return String.valueOf(num);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) {
            return "***@" + parts[1];
        }
        return local.charAt(0) + "***@" + parts[1];
    }
}