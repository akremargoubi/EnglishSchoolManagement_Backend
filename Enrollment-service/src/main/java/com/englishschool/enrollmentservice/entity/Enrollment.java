package com.englishschool.enrollmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String studentName;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "enrolled_at", updatable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (enrolledAt == null) enrolledAt = Instant.now();
    }
}
