package com.esprit.esmauthms.controller;

import com.esprit.esmauthms.dto.StudentClassRequest;
import com.esprit.esmauthms.dto.StudentClassResponseDto;
import com.esprit.esmauthms.service.StudentClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class StudentClassController {

    private final StudentClassService studentClassService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public StudentClassResponseDto create(@RequestBody StudentClassRequest request) {
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
                                          @RequestBody StudentClassRequest request) {
        return studentClassService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        studentClassService.delete(id);
    }

    // ── Assign / Remove student ───────────────────────────────────────────────

    /**
     * Assigne un étudiant à une classe.
     * PUT /api/classes/{classId}/students/{userId}
     */
    @PutMapping("/{classId}/students/{userId}")
    public void assignStudent(@PathVariable Long classId,
                              @PathVariable UUID userId) {
        studentClassService.assignStudent(classId, userId);
    }

    /**
     * Retire un étudiant d'une classe.
     * DELETE /api/classes/{classId}/students/{userId}
     */
    @DeleteMapping("/{classId}/students/{userId}")
    public void removeStudent(@PathVariable Long classId,
                              @PathVariable UUID userId) {
        studentClassService.removeStudent(classId, userId);
    }
}