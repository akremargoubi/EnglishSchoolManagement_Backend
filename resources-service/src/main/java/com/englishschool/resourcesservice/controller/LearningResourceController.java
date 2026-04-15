package com.englishschool.resourcesservice.controller;

import com.englishschool.resourcesservice.entity.LearningResource;
import com.englishschool.resourcesservice.service.LearningResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class LearningResourceController {

    private final LearningResourceService service;

    public LearningResourceController(LearningResourceService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("title") String title,
            @RequestParam("type") String type,
            @RequestParam("published") boolean published,
            @RequestParam("assessmentId") Long assessmentId,
            @RequestParam("file") MultipartFile file) {

        service.upload(title, type, published, assessmentId, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<LearningResource>> getByAssessment(
            @PathVariable Long assessmentId) {
        return ResponseEntity.ok(service.getByAssessmentId(assessmentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}