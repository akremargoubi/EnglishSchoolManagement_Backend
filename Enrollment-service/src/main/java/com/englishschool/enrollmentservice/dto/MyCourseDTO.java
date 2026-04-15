package com.englishschool.enrollmentservice.dto;

import com.englishschool.enrollmentservice.client.CourseDTO;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCourseDTO {
    private Long enrollmentId;
    private Long courseId;
    private String status;
    private Integer progressPercent;
    private Instant enrolledAt;
    private Instant completedAt;
    private CourseDTO course;
}
