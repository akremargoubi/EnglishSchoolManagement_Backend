package com.englishschool.courseservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating; // 1-5

    private String comment;

    @Column(name = "created_at", updatable = false)
    private java.time.Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // rating constraints
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}
