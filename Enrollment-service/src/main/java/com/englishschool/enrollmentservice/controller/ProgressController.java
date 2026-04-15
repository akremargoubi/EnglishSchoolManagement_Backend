package com.englishschool.enrollmentservice.controller;

import com.englishschool.enrollmentservice.dto.ProgressDTO;
import com.englishschool.enrollmentservice.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Lesson completion and course progress tracking")
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/enrollments/{enrollmentId}/lessons/{lessonId}/complete")
    @Operation(summary = "Mark lesson as complete")
    public ProgressDTO markLessonComplete(
            @PathVariable Long enrollmentId,
            @PathVariable Long lessonId) {
        return progressService.markLessonComplete(enrollmentId, lessonId);
    }

    @GetMapping("/enrollments/{enrollmentId}/lessons")
    @Operation(summary = "Get completed lessons for enrollment")
    public List<ProgressDTO> getLessonProgress(@PathVariable Long enrollmentId) {
        return progressService.getLessonProgress(enrollmentId);
    }

    @GetMapping("/enrollments/{enrollmentId}/percent")
    @Operation(summary = "Get course completion percentage")
    public Map<String, Integer> getCourseProgressPercent(@PathVariable Long enrollmentId) {
        int percent = progressService.getCourseProgressPercent(enrollmentId);
        return Map.of("progressPercent", percent);
    }
}
