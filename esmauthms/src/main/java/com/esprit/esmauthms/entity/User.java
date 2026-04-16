package com.esprit.esmauthms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private String uuid;

    @Column(name = "cin", unique = true, length = 8)
    private String cin;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private String role = "USER";

    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String phoneNumber;
    private String address;
    private String status;

    // 🆕 Relation vers la classe de l'étudiant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private StudentClass studentClass;

    private boolean isEmailVerified;
    private boolean twoFactorEnabled;
    private String twoFactorSecret;
    private String twoFactorCode;
    private LocalDateTime twoFactorCodeExpiresAt;
    private String passwordResetToken;
    private LocalDateTime passwordResetExpiresAt;
    private String emailVerificationToken;
    private LocalDateTime emailVerificationExpiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}