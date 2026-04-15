package com.example.assessmentservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Course name is required")
    private String courseName;

    @Enumerated(EnumType.STRING)
    @NotNull
    private AssessmentType type;

    @Enumerated(EnumType.STRING)
    @NotNull
    private AssessmentStatus status;

    // ── Planning fields ────────────────────────────────────────────────────────
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer duration; // en minutes

    // Champ calculé : retourne true si l'assessment est à venir
    @Transient
    public boolean isUpcoming() {
        return startDate != null && startDate.isAfter(LocalDateTime.now());
    }

    // Champ calculé : retourne true si l'assessment est en cours
    @Transient
    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return startDate != null && endDate != null
                && startDate.isBefore(now) && endDate.isAfter(now);
    }
}