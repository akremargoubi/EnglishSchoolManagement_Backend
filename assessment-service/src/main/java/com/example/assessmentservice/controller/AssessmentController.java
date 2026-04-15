package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService service;

    @PostMapping
    public Assessment create(@Valid @RequestBody Assessment assessment) {
        return service.create(assessment);
    }

    @GetMapping
    public List<Assessment> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Assessment getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Assessment update(@PathVariable Long id,
                             @Valid @RequestBody Assessment assessment) {
        return service.update(id, assessment);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}