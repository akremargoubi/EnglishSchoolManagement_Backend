package com.example.assessmentservice.service;

import com.example.assessmentservice.entity.Grade;
import com.example.assessmentservice.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    // ── CRUD ───────────────────────────────────────────────────────────────────
    public Grade create(Grade grade) {
        return gradeRepository.save(grade);
    }

    public List<Grade> getAll() {
        return gradeRepository.findAll();
    }

    public Grade getById(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grade not found: " + id));
    }

    public List<Grade> getByAssessment(Long assessmentId) {
        return gradeRepository.findByAssessmentId(assessmentId);
    }

    public List<Grade> getByStudent(String studentEmail) {
        return gradeRepository.findByStudentEmail(studentEmail);
    }

    public Grade update(Long id, Grade updated) {
        Grade existing = getById(id);
        existing.setStudentName(updated.getStudentName());
        existing.setStudentEmail(updated.getStudentEmail());
        existing.setScore(updated.getScore());
        existing.setMaxScore(updated.getMaxScore());
        existing.setComments(updated.getComments());
        return gradeRepository.save(existing);
    }

    public void delete(Long id) {
        gradeRepository.deleteById(id);
    }

    // ── Statistics ─────────────────────────────────────────────────────────────
    public Map<String, Object> getStatsByAssessment(Long assessmentId) {
        List<Grade> grades = gradeRepository.findByAssessmentId(assessmentId);
        long total   = grades.size();
        long passing = gradeRepository.countPassing(assessmentId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total",    total);
        stats.put("average",  gradeRepository.getAverageByAssessment(assessmentId));
        stats.put("max",      gradeRepository.getMaxByAssessment(assessmentId));
        stats.put("min",      gradeRepository.getMinByAssessment(assessmentId));
        stats.put("passing",  passing);
        stats.put("failing",  total - passing);
        stats.put("passRate", total > 0 ? Math.round((passing * 100.0) / total) : 0);
        return stats;
    }

    // ── Leaderboard ────────────────────────────────────────────────────────────

    /**
     * Global leaderboard — average percentage across ALL assessments per student
     */
    public List<Map<String, Object>> getGlobalLeaderboard() {
        List<Grade> all = gradeRepository.findAll();

        // Group by student email
        Map<String, List<Grade>> byStudent = all.stream()
                .collect(Collectors.groupingBy(Grade::getStudentEmail));

        List<Map<String, Object>> leaderboard = new ArrayList<>();

        byStudent.forEach((email, grades) -> {
            double totalPct = grades.stream()
                    .filter(g -> g.getMaxScore() != null && g.getMaxScore() > 0)
                    .mapToDouble(g -> (g.getScore() / g.getMaxScore()) * 100)
                    .average()
                    .orElse(0.0);

            double avgScore = grades.stream()
                    .mapToDouble(Grade::getScore)
                    .average()
                    .orElse(0.0);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("studentName",       grades.get(0).getStudentName());
            entry.put("studentEmail",      email);
            entry.put("averagePercentage", Math.round(totalPct * 10.0) / 10.0);
            entry.put("averageScore",      Math.round(avgScore * 10.0) / 10.0);
            entry.put("totalAssessments",  grades.size());
            entry.put("mention",           getMention(totalPct));
            leaderboard.add(entry);
        });

        // Sort by averagePercentage descending
        leaderboard.sort((a, b) ->
                Double.compare((Double) b.get("averagePercentage"), (Double) a.get("averagePercentage")));

        // Add rank
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1);
        }

        return leaderboard;
    }

    /**
     * Leaderboard for a specific assessment
     */
    public List<Map<String, Object>> getLeaderboardByAssessment(Long assessmentId) {
        List<Grade> grades = gradeRepository.findByAssessmentId(assessmentId);

        List<Map<String, Object>> leaderboard = grades.stream()
                .sorted(Comparator.comparingDouble(Grade::getScore).reversed())
                .map(g -> {
                    double pct = g.getMaxScore() != null && g.getMaxScore() > 0
                            ? (g.getScore() / g.getMaxScore()) * 100 : 0;
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("studentName",  g.getStudentName());
                    entry.put("studentEmail", g.getStudentEmail());
                    entry.put("score",        g.getScore());
                    entry.put("maxScore",     g.getMaxScore());
                    entry.put("percentage",   Math.round(pct * 10.0) / 10.0);
                    entry.put("mention",      getMention(pct));
                    entry.put("comments",     g.getComments());
                    return entry;
                })
                .collect(Collectors.toList());

        // Add rank
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1);
        }

        return leaderboard;
    }

    private String getMention(double percentage) {
        if (percentage >= 90) return "EXCELLENT";
        if (percentage >= 75) return "GOOD";
        if (percentage >= 60) return "AVERAGE";
        return "FAIL";
    }
}