package com.englishschool.resourcesservice.controller;

import com.englishschool.resourcesservice.entity.LearningResource;
import com.englishschool.resourcesservice.security.CallerContext;
import com.englishschool.resourcesservice.service.LearningResourceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class LearningResourceController {

    private final LearningResourceService service;

    @PostMapping("/upload")
    public ResponseEntity<LearningResource> upload(
            @RequestParam("title") String title,
            @RequestParam("type") String type,
            @RequestParam("published") boolean published,
            @RequestParam("assessmentId") Long assessmentId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest req) {
        return ResponseEntity.ok(service.upload(title, type, published, assessmentId, file, caller(req)));
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<LearningResource>> getByAssessment(
            @PathVariable Long assessmentId, HttpServletRequest req) {
        return ResponseEntity.ok(service.getByAssessmentId(assessmentId, caller(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest req) {
        service.delete(id, caller(req));
        return ResponseEntity.noContent().build();
    }

    private CallerContext caller(HttpServletRequest req) {
        return new CallerContext(
                (UUID) req.getAttribute("callerId"),
                (String) req.getAttribute("callerRole"),
                (String) req.getAttribute("callerEmail"));
    }
}
