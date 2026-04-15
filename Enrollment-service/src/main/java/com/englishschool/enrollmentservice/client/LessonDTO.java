package com.englishschool.enrollmentservice.client;

import lombok.Data;

@Data
public class LessonDTO {
    private Long id;
    private Long moduleId;
    private String title;
    private String contentType;
    private String contentUrl;
    private Integer durationMinutes;
    private Integer orderIndex;
}
