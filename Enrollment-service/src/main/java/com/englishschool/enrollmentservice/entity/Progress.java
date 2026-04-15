package com.englishschool.enrollmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "completed_at", updatable = false)
    private java.time.Instant completedAt;

    @PrePersist
    protected void onCreate() {
        completedAt = java.time.Instant.now();
    }
}
