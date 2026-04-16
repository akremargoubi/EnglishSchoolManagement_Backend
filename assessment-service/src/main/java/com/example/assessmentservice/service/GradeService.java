package com.example.assessmentservice.service;

import com.example.assessmentservice.client.CertificateServiceClient;
import com.example.assessmentservice.entity.Grade;
import com.example.assessmentservice.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeService {

    private final GradeRepository gradeRepository;
    private final CertificateServiceClient certificateClient;

    // Seuil de réussite : 50%
    private static final double PASS_THRESHOLD = 0.50;

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public Grade create(Grade grade) {
        Grade saved = gradeRepository.save(grade);

        // Déclenche la génération du certificat si l'étudiant a réussi
        if (saved.getMaxScore() != null && saved.getMaxScore() > 0
                && (saved.getScore() / saved.getMaxScore()) >= PASS_THRESHOLD) {

            log.info("🎓 Étudiant {} a réussi avec {}/{}. Génération du certificat...",
                    saved.getStudentName(), saved.getScore(), saved.getMaxScore());

            certificateClient.generateCertificateAsync(
                    new CertificateServiceClient.CertificateRequest(
                            saved.getStudentName(),
                            saved.getStudentEmail(),
                            "Assessment #" + saved.getAssessmentId(),
                            saved.getScore(),
                            saved.getMaxScore(),
                            Instant.now().toString()
                    )
            );
        }

        return saved;
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

    public List<Map<String, Object>> getGlobalLeaderboard() {
        List<Grade> all = gradeRepository.findAll();

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

        leaderboard.sort((a, b) ->
                Double.compare((Double) b.get("averagePercentage"), (Double) a.get("averagePercentage")));

        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1);
        }

        return leaderboard;
    }

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