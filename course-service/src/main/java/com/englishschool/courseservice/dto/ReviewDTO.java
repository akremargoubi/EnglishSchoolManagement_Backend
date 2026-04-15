package com.englishschool.courseservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private Long id;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 2000)
    private String comment;

    private Instant createdAt;
}
