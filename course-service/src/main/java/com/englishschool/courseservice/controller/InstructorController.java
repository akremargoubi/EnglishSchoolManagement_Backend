package com.englishschool.courseservice.controller;

import com.englishschool.courseservice.dto.InstructorDTO;
import com.englishschool.courseservice.service.InstructorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instructors")
@RequiredArgsConstructor
@Tag(name = "Instructors", description = "Instructor CRUD API")
public class InstructorController {

    private final InstructorService service;

    @GetMapping
    @Operation(summary = "Get all instructors")
    public List<InstructorDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get instructor by ID")
    public InstructorDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create instructor")
    public InstructorDTO create(@Valid @RequestBody InstructorDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update instructor")
    public InstructorDTO update(@PathVariable Long id, @Valid @RequestBody InstructorDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete instructor")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
