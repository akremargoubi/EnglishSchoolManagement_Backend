package com.englishschool.enrollmentservice.client;

import lombok.Data;

import java.util.List;

@Data
public class ModuleDTO {
    private Long id;
    private Long courseId;
    private String title;
    private Integer orderIndex;
    private List<LessonDTO> lessons;
}
