package com.esprit.esmauthms.controller;

import com.esprit.esmauthms.dto.StudentClassRequest;
import com.esprit.esmauthms.dto.StudentClassResponseDto;
import com.esprit.esmauthms.dto.StudentClassResponseDto.StudentSummary;
import com.esprit.esmauthms.service.StudentClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/classes")   // context-path=/api, so routes are /api/classes/**
@RequiredArgsConstructor
public class StudentClassController {

    private final StudentClassService studentClassService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Create a class. Optionally include tutorId to assign a tutor immediately. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentClassResponseDto create(@Valid @RequestBody StudentClassRequest request) {
        return studentClassService.create(request);
    }

    @GetMapping
    public List<StudentClassResponseDto> getAll() {
        return studentClassService.getAll();
    }

    @GetMapping("/{id}")
    public StudentClassResponseDto getById(@PathVariable Long id) {
        return studentClassService.getById(id);
    }

    @PutMapping("/{id}")
    public StudentClassResponseDto update(@PathVariable Long id,
                                          @Valid @RequestBody StudentClassRequest request) {
        return studentClassService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentClassService.delete(id);
    }

    // ── Students ──────────────────────────────────────────────────────────────

    /** List all students in a class. */
    @GetMapping("/{classId}/students")
    public List<StudentSummary> getStudents(@PathVariable Long classId) {
        return studentClassService.getStudents(classId);
    }

    /** Assign a student to a class. Returns the updated class. */
    @PutMapping("/{classId}/students/{userId}")
    public StudentClassResponseDto assignStudent(@PathVariable Long classId,
                                                 @PathVariable UUID userId) {
        return studentClassService.assignStudent(classId, userId);
    }

    /** Remove a student from a class. Returns the updated class. */
    @DeleteMapping("/{classId}/students/{userId}")
    public StudentClassResponseDto removeStudent(@PathVariable Long classId,
                                                 @PathVariable UUID userId) {
        return studentClassService.removeStudent(classId, userId);
    }

    // ── Tutor ─────────────────────────────────────────────────────────────────

    /** Assign a tutor to a class. */
    @PutMapping("/{classId}/tutor/{tutorId}")
    public StudentClassResponseDto assignTutor(@PathVariable Long classId,
                                               @PathVariable UUID tutorId) {
        return studentClassService.assignTutor(classId, tutorId);
    }

    /** Remove the tutor from a class. */
    @DeleteMapping("/{classId}/tutor")
    public StudentClassResponseDto removeTutor(@PathVariable Long classId) {
        return studentClassService.removeTutor(classId);
    }
}
