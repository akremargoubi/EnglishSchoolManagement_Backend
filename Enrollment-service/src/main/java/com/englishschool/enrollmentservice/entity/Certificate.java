package com.englishschool.enrollmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false, unique = true)
    private Enrollment enrollment;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "issued_at", updatable = false)
    private java.time.Instant issuedAt;

    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;

    @PrePersist
    protected void onCreate() {
        issuedAt = java.time.Instant.now();
    }
}
