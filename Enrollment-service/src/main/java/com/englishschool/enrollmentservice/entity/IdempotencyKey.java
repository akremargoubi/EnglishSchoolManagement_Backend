package com.englishschool.enrollmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores idempotency keys for idempotent API calls (e.g. enroll).
 */
@Entity
@Table(name = "idempotency_keys", indexes = @Index(unique = true, columnList = "key_hash"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
