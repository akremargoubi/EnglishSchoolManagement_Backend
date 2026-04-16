package com.esprit.esmauthms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponseDto {

    private UUID id;
    private String uuid;
    private String cin;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String phoneNumber;
    private String address;
    private String status;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private Instant createdAt;
    private Instant updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;

    // 🆕 Classe de l'étudiant
    private Long classId;
    private String className;
    private String classLevel;
    private String classSpecialty;
}