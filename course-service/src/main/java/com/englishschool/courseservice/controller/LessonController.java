package com.englishschool.courseservice.controller;

import com.englishschool.courseservice.dto.LessonDTO;
import com.englishschool.courseservice.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lessons", description = "Lesson CRUD API - Video, PDF, Quiz content")
public class LessonController {

    private final LessonService service;

    @GetMapping(params = "moduleId")
    @Operation(summary = "Get lessons by module ID")
    public List<LessonDTO> getByModuleId(@RequestParam Long moduleId) {
        return service.getByModuleId(moduleId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lesson by ID")
    public LessonDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create lesson (VIDEO, PDF, QUIZ, TEXT)")
    public LessonDTO create(@Valid @RequestBody LessonDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lesson")
    public LessonDTO update(@PathVariable Long id, @Valid @RequestBody LessonDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete lesson")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
