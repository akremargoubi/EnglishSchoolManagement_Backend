package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.Grade;
import com.example.assessmentservice.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping
    public ResponseEntity<List<Grade>> getAll() {
        return ResponseEntity.ok(gradeService.getAll());
    }

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<Grade>> getByAssessment(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(gradeService.getByAssessment(assessmentId));
    }

    @GetMapping("/student/{email}")
    public ResponseEntity<List<Grade>> getByStudent(@PathVariable String email) {
        return ResponseEntity.ok(gradeService.getByStudent(email));
    }

    @GetMapping("/assessment/{assessmentId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(gradeService.getStatsByAssessment(assessmentId));
    }

    // ── Leaderboard ────────────────────────────────────────────────────────────
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getGlobalLeaderboard() {
        return ResponseEntity.ok(gradeService.getGlobalLeaderboard());
    }

    @GetMapping("/leaderboard/assessment/{assessmentId}")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboardByAssessment(
            @PathVariable Long assessmentId) {
        return ResponseEntity.ok(gradeService.getLeaderboardByAssessment(assessmentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grade> getById(@PathVariable Long id) {
        return ResponseEntity.ok(gradeService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Grade> create(@RequestBody Grade grade) {
        return ResponseEntity.ok(gradeService.create(grade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Grade> update(@PathVariable Long id, @RequestBody Grade grade) {
        return ResponseEntity.ok(gradeService.update(id, grade));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gradeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}