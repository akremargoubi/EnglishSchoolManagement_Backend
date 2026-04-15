package com.englishschool.courseservice.dto;

import com.englishschool.courseservice.entity.Lesson;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDTO {

    private Long id;

    @NotNull(message = "Module ID is required")
    private Long moduleId;

    @NotBlank(message = "Lesson title is required")
    @Size(max = 255)
    private String title;

    private Lesson.ContentType contentType;

    @Size(max = 500)
    private String contentUrl;

    private String contentText;

    private String quizContentJson;

    @Min(0)
    private Integer durationMinutes;

    @Min(0)
    private Integer orderIndex;
}
