package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.security.CallerContext;
import com.example.assessmentservice.service.AssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService service;

    @PostMapping
    public Assessment create(@Valid @RequestBody Assessment assessment, HttpServletRequest req) {
        return service.create(assessment, caller(req));
    }

    @GetMapping
    public List<Assessment> getAll(HttpServletRequest req) {
        return service.getAll(caller(req));
    }

    @GetMapping("/{id}")
    public Assessment getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/class/{className}")
    public List<Assessment> getByClassName(@PathVariable String className, HttpServletRequest req) {
        return service.getByClassName(className, caller(req));
    }

    @PutMapping("/{id}")
    public Assessment update(@PathVariable Long id,
                             @Valid @RequestBody Assessment assessment,
                             HttpServletRequest req) {
        return service.update(id, assessment, caller(req));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest req) {
        service.delete(id, caller(req));
    }

    private CallerContext caller(HttpServletRequest req) {
        return new CallerContext(
                (UUID) req.getAttribute("callerId"),
                (String) req.getAttribute("callerRole"),
                (String) req.getAttribute("callerEmail"));
    }
}
