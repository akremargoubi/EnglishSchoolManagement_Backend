package com.englishschool.enrollmentservice.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {
    private Long id;
    private Long enrollmentId;
    private Long lessonId;
    private Instant completedAt;
}
