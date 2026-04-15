package com.example.assessmentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "grade")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long assessmentId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String studentEmail;

    private Double score;

    private Double maxScore;

    private String comments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime gradedAt;

    @PrePersist
    public void prePersist() {
        this.gradedAt = LocalDateTime.now();
    }

    // Calcul automatique du pourcentage
    @Transient
    public Double getPercentage() {
        if (maxScore == null || maxScore == 0) return null;
        return Math.round((score / maxScore) * 1000.0) / 10.0;
    }

    // Mention selon le score
    @Transient
    public String getMention() {
        Double pct = getPercentage();
        if (pct == null) return "N/A";
        if (pct >= 90) return "EXCELLENT";
        if (pct >= 75) return "GOOD";
        if (pct >= 60) return "AVERAGE";
        return "FAIL";
    }
}