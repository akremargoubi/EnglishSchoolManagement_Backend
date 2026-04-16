package com.example.assessmentservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
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

    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    private AssessmentType type;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private AssessmentStatus status;

    // Groupe d'étudiants ciblé (ex: "TWIN1", "TWIN2", "DS3", etc.)
    private String className;

    private String startDate;
    private String endDate;
    private Integer duration;
}