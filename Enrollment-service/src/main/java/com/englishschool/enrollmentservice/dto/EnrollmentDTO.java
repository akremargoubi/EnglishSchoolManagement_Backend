package com.englishschool.enrollmentservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {

    private Long id;

    private Long userId;

    private String studentName;

    @NotNull
    private Long courseId;

    private String status;

    private Instant enrolledAt;

    private Instant completedAt;

    private Integer progressPercent;
}
