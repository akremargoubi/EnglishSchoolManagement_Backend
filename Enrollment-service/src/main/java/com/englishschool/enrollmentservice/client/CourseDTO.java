package com.englishschool.enrollmentservice.client;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseDTO {
    private Long courseId;
    private String name;
    private String level;
    private String description;
    private Long categoryId;
    private Long instructorId;
    private BigDecimal price;
    private String thumbnailUrl;
    private Boolean isPublished;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
}
