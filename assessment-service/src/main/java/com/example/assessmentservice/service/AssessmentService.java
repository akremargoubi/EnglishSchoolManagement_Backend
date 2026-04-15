package com.example.assessmentservice.service;

import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository repository;

    // ── CRUD de base ───────────────────────────────────────────────────────────
    public Assessment create(Assessment assessment) {
        return repository.save(assessment);
    }

    public List<Assessment> getAll() {
        return repository.findAll();
    }

    public Assessment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found: " + id));
    }

    public Assessment update(Long id, Assessment updated) {
        Assessment existing = getById(id);
        existing.setTitle(updated.getTitle());
        existing.setCourseName(updated.getCourseName());
        existing.setType(updated.getType());
        existing.setStatus(updated.getStatus());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setDuration(updated.getDuration());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    // ── Planning ───────────────────────────────────────────────────────────────

    // Assessments d'un mois (year=2026, month=3)
    public List<Assessment> getByMonth(int year, int month) {
        YearMonth ym    = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end   = ym.atEndOfMonth().atTime(23, 59, 59);
        return repository.findByMonth(start, end);
    }

    // Prochains assessments
    public List<Assessment> getUpcoming() {
        return repository.findUpcoming(LocalDateTime.now());
    }

    // Assessments en cours
    public List<Assessment> getOngoing() {
        return repository.findOngoing(LocalDateTime.now());
    }

    // Assessments par plage de dates
    public List<Assessment> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByDateRange(start, end);
    }
}