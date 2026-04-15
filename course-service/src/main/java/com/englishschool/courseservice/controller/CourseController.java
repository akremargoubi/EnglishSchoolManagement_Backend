package com.englishschool.courseservice.controller;

import com.englishschool.courseservice.dto.CourseDTO;
import com.englishschool.courseservice.dto.CourseSearchRequest;
import com.englishschool.courseservice.dto.PageResponse;
import com.englishschool.courseservice.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course CRUD API")
public class CourseController {

    private final CourseService service;

    @GetMapping("/test")
    @Operation(summary = "Health check")
    public String test() {
        return "Course Service is working";
    }

    @GetMapping
    @Operation(summary = "Get all courses (or search with query params)")
    public Object getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long instructorId,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean freeOnly,
            @RequestParam(required = false, defaultValue = "courseId") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        if (search != null || level != null || categoryId != null || instructorId != null
                || isPublished != null || minRating != null || freeOnly != null || page > 0 || size != 20) {
            CourseSearchRequest req = CourseSearchRequest.builder()
                    .search(search).level(level).categoryId(categoryId).instructorId(instructorId)
                    .isPublished(isPublished).minRating(minRating).freeOnly(freeOnly).sortBy(sortBy).sortDir(sortDir)
                    .page(page).size(size).build();
            return service.search(req);
        }
        return service.getAll();
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID")
    public CourseDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create course")
    public CourseDTO create(@Valid @RequestBody CourseDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update course")
    public CourseDTO update(@PathVariable Long id, @Valid @RequestBody CourseDTO dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish course")
    public CourseDTO publish(@PathVariable Long id) {
        return service.publish(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete course")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
