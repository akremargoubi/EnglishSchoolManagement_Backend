package com.englishschool.enrollmentservice.controller;

import com.englishschool.enrollmentservice.dto.EnrollmentDTO;
import com.englishschool.enrollmentservice.dto.MyCourseDTO;
import com.englishschool.enrollmentservice.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment API")
public class EnrollmentController {

    private final EnrollmentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enroll in course (idempotent with X-Idempotency-Key header)")
    public EnrollmentDTO enroll(
            @Valid @RequestBody EnrollmentDTO dto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return service.enroll(dto, idempotencyKey);
    }

    @GetMapping
    @Operation(summary = "Get all enrollments (ADMIN)")
    public List<EnrollmentDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get enrollment by ID")
    public EnrollmentDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/user/{userId}/my-courses")
    @Operation(summary = "Student dashboard - My Courses")
    public List<MyCourseDTO> getMyCourses(@PathVariable Long userId) {
        return service.getMyCourses(userId);
    }

    @GetMapping("/user/{userId}/history")
    @Operation(summary = "Enrollment history")
    public List<EnrollmentDTO> getEnrollmentHistory(@PathVariable Long userId) {
        return service.getEnrollmentHistory(userId);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get enrollments by user ID")
    public List<EnrollmentDTO> getByUserId(@PathVariable Long userId) {
        return service.getByUserId(userId);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel enrollment")
    public EnrollmentDTO cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update enrollment")
    public EnrollmentDTO update(@PathVariable Long id, @Valid @RequestBody EnrollmentDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete enrollment")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
