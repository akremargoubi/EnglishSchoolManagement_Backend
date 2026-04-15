package com.englishschool.courseservice.controller;

import com.englishschool.courseservice.dto.ModuleDTO;
import com.englishschool.courseservice.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/modules")
@RequiredArgsConstructor
@Tag(name = "Modules", description = "Module CRUD API - Course hierarchy")
public class ModuleController {

    private final ModuleService service;

    @GetMapping(params = "courseId")
    @Operation(summary = "Get modules by course ID")
    public List<ModuleDTO> getByCourseId(
            @RequestParam Long courseId,
            @RequestParam(required = false, defaultValue = "false") boolean includeLessons) {
        return service.getByCourseId(courseId, includeLessons);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get module by ID")
    public ModuleDTO getById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeLessons) {
        return service.getById(id, includeLessons);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create module")
    public ModuleDTO create(@Valid @RequestBody ModuleDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update module")
    public ModuleDTO update(@PathVariable Long id, @Valid @RequestBody ModuleDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete module")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
