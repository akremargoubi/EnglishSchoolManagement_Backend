package com.englishschool.courseservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDTO {

    private Long id;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "Module title is required")
    @Size(max = 255)
    private String title;

    @Min(0)
    private Integer orderIndex;

    private List<LessonDTO> lessons;
}
