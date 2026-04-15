package com.englishschool.courseservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private Long courseId;

    @NotBlank(message = "Course name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Level is required")
    @Pattern(regexp = "A1|A2|B1|B2|C1|C2", message = "Level must be A1, A2, B1, B2, C1, or C2")
    private String level;

    private String description;

    private Long categoryId;

    private Long instructorId;

    @DecimalMin(value = "0", message = "Price cannot be negative")
    private BigDecimal price;

    private String thumbnailUrl;

    private Boolean isPublished;

    private BigDecimal ratingAvg;

    private Integer ratingCount;
}
