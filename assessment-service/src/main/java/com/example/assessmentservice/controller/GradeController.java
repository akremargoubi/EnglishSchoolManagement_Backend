package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.Grade;
import com.example.assessmentservice.security.CallerContext;
import com.example.assessmentservice.service.GradeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping
    public ResponseEntity<List<Grade>> getAll(HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.getAll(caller(req)));
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<Grade>> getByAssessment(@PathVariable Long assessmentId, HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.getByAssessment(assessmentId, caller(req)));
    }

    @GetMapping("/student/{email}")
    public ResponseEntity<List<Grade>> getByStudent(@PathVariable String email, HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.getByStudent(email, caller(req)));
    }

    @GetMapping("/assessment/{assessmentId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long assessmentId, HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.getStatsByAssessment(assessmentId, caller(req)));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getGlobalLeaderboard(HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.getGlobalLeaderboard(caller(req)));
    }

    @GetMapping("/leaderboard/assessment/{assessmentId}")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboardByAssessment(
            @PathVariable Long assessmentId, HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.getLeaderboardByAssessment(assessmentId, caller(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grade> getById(@PathVariable Long id) {
        return ResponseEntity.ok(gradeService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Grade> create(@RequestBody Grade grade, HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.create(grade, caller(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Grade> update(@PathVariable Long id, @RequestBody Grade grade, HttpServletRequest req) {
        return ResponseEntity.ok(gradeService.update(id, grade, caller(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        gradeService.delete(id, caller(req));
        return ResponseEntity.noContent().build();
    }

    private CallerContext caller(HttpServletRequest req) {
        return new CallerContext(
                (UUID) req.getAttribute("callerId"),
                (String) req.getAttribute("callerRole"),
                (String) req.getAttribute("callerEmail"));
    }
}
