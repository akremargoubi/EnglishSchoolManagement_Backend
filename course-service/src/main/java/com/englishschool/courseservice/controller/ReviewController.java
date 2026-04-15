package com.englishschool.courseservice.controller;

import com.englishschool.courseservice.dto.PageResponse;
import com.englishschool.courseservice.dto.ReviewDTO;
import com.englishschool.courseservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Course rating & reviews API")
public class ReviewController {

    private final ReviewService service;

    @GetMapping(params = "courseId")
    @Operation(summary = "Get reviews by course ID (paginated)")
    public PageResponse<ReviewDTO> getByCourseId(
            @RequestParam Long courseId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return service.getByCourseId(courseId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID")
    public ReviewDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create review (rating 1-5)")
    public ReviewDTO create(@Valid @RequestBody ReviewDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update review")
    public ReviewDTO update(@PathVariable Long id, @Valid @RequestBody ReviewDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete review")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
