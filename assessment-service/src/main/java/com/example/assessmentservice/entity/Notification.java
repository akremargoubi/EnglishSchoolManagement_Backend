package com.example.assessmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // ASSESSMENT_CREATED, RESOURCE_ADDED

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    private String targetEmail; // null = broadcast to all

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long relatedId; // assessmentId or resourceId

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}