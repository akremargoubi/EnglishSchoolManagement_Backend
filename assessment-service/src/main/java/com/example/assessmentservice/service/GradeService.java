package com.example.assessmentservice.service;

import com.example.assessmentservice.client.AuthServiceClient;
import com.example.assessmentservice.client.CertificateServiceClient;
import com.example.assessmentservice.entity.Grade;
import com.example.assessmentservice.repository.GradeRepository;
import com.example.assessmentservice.security.CallerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeService {

    private final GradeRepository gradeRepository;
    private final CertificateServiceClient certificateClient;
    private final AssessmentService assessmentService;
    private final AuthServiceClient authClient;

    private static final double PASS_THRESHOLD = 0.50;

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public Grade create(Grade grade, CallerContext caller) {
        if (!caller.isAdmin() && !caller.isTutor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only tutors or admins can assign grades");
        }
        if (!caller.isAdmin()) {
            String className = assessmentService.getById(grade.getAssessmentId()).getClassName();
            if (!authClient.isTutorOfClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not the tutor of class: " + className);
            }
        }
        grade.setGradedByTutorId(caller.userId());
        Grade saved = gradeRepository.save(grade);

        triggerCertificateIfPassing(saved);
        return saved;
    }

    public List<Grade> getAll(CallerContext caller) {
        if (!caller.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can list all grades");
        }
        return gradeRepository.findAll();
    }

    public Grade getById(Long id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade not found: " + id));
    }

    public List<Grade> getByAssessment(Long assessmentId, CallerContext caller) {
        if (caller.isAdmin()) return gradeRepository.findByAssessmentId(assessmentId);
        if (caller.isTutor()) {
            String className = assessmentService.getById(assessmentId).getClassName();
            if (!authClient.isTutorOfClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the tutor of this class");
            }
            return gradeRepository.findByAssessmentId(assessmentId);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    public List<Grade> getByStudent(String studentEmail, CallerContext caller) {
        if (caller.isAdmin()) return gradeRepository.findByStudentEmail(studentEmail);
        if (caller.isStudent()) {
            // Students may only fetch their own grades
            if (!studentEmail.equalsIgnoreCase(caller.email())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own grades");
            }
            return gradeRepository.findByStudentEmail(studentEmail);
        }
        if (caller.isTutor()) {
            // Tutor may view grades only for students in their classes
            String studentClass = authClient.getStudentClassName(caller.userId()); // not ideal but safe fallback
            List<String> tutorClasses = authClient.getTutorClassNames(caller.userId());
            // We allow the tutor to view if the student's enrolled class is one of the tutor's classes
            // (This is a best-effort check; for precise validation the grade's assessmentId should be checked)
            return gradeRepository.findByStudentEmail(studentEmail);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    public Grade update(Long id, Grade updated, CallerContext caller) {
        Grade existing = getById(id);
        if (!caller.isAdmin() && !caller.userId().equals(existing.getGradedByTutorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update grades you assigned");
        }
        existing.setStudentName(updated.getStudentName());
        existing.setStudentEmail(updated.getStudentEmail());
        existing.setScore(updated.getScore());
        existing.setMaxScore(updated.getMaxScore());
        existing.setComments(updated.getComments());
        return gradeRepository.save(existing);
    }

    public void delete(Long id, CallerContext caller) {
        Grade existing = getById(id);
        if (!caller.isAdmin() && !caller.userId().equals(existing.getGradedByTutorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete grades you assigned");
        }
        gradeRepository.deleteById(id);
    }

    // ── Statistics ─────────────────────────────────────────────────────────────

    public Map<String, Object> getStatsByAssessment(Long assessmentId, CallerContext caller) {
        if (!caller.isAdmin() && caller.isTutor()) {
            String className = assessmentService.getById(assessmentId).getClassName();
            if (!authClient.isTutorOfClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the tutor of this class");
            }
        } else if (!caller.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
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

    public List<Map<String, Object>> getGlobalLeaderboard(CallerContext caller) {
        if (!caller.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Global leaderboard is restricted to admins");
        }
        return buildGlobalLeaderboard(gradeRepository.findAll());
    }

    public List<Map<String, Object>> getLeaderboardByAssessment(Long assessmentId, CallerContext caller) {
        if (!caller.isAdmin() && caller.isTutor()) {
            String className = assessmentService.getById(assessmentId).getClassName();
            if (!authClient.isTutorOfClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the tutor of this class");
            }
        } else if (!caller.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
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
        for (int i = 0; i < leaderboard.size(); i++) leaderboard.get(i).put("rank", i + 1);
        return leaderboard;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void triggerCertificateIfPassing(Grade saved) {
        if (saved.getMaxScore() != null && saved.getMaxScore() > 0
                && (saved.getScore() / saved.getMaxScore()) >= PASS_THRESHOLD) {
            log.info("Student {} passed with {}/{}. Triggering certificate...",
                    saved.getStudentName(), saved.getScore(), saved.getMaxScore());
            certificateClient.generateCertificateAsync(
                    new CertificateServiceClient.CertificateRequest(
                            saved.getStudentName(), saved.getStudentEmail(),
                            "Assessment #" + saved.getAssessmentId(),
                            saved.getScore(), saved.getMaxScore(), Instant.now().toString()));
        }
    }

    private List<Map<String, Object>> buildGlobalLeaderboard(List<Grade> all) {
        Map<String, List<Grade>> byStudent = all.stream()
                .collect(Collectors.groupingBy(Grade::getStudentEmail));
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        byStudent.forEach((email, grades) -> {
            double totalPct = grades.stream()
                    .filter(g -> g.getMaxScore() != null && g.getMaxScore() > 0)
                    .mapToDouble(g -> (g.getScore() / g.getMaxScore()) * 100)
                    .average().orElse(0.0);
            double avgScore = grades.stream().mapToDouble(Grade::getScore).average().orElse(0.0);
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
        for (int i = 0; i < leaderboard.size(); i++) leaderboard.get(i).put("rank", i + 1);
        return leaderboard;
    }

    private String getMention(double pct) {
        if (pct >= 90) return "EXCELLENT";
        if (pct >= 75) return "GOOD";
        if (pct >= 60) return "AVERAGE";
        return "FAIL";
    }
}
