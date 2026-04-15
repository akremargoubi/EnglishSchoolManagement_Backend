package com.englishschool.enrollmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Aggregated course progress per enrollment (total lessons, completed count, %).
 * Updated when lessons are marked complete.
 */
@Entity
@Table(name = "student_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false, unique = true)
    private Enrollment enrollment;

    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    private Integer totalLessons = 0;

    @Column(name = "completed_lessons", nullable = false)
    @Builder.Default
    private Integer completedLessons = 0;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
