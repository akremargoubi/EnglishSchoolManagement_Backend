package com.example.assessmentservice.service;

import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository repository;

    public Assessment create(Assessment assessment) {
        return repository.save(assessment);
    }

    public List<Assessment> getAll() {
        return repository.findAll();
    }

    public Assessment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found with id " + id));
    }

    /**
     * Retourne tous les assessments d'une classe spécifique.
     * Utilisé par le dashboard étudiant pour filtrer par className.
     */
    public List<Assessment> getByClassName(String className) {
        return repository.findByClassName(className);
    }

    public Assessment update(Long id, Assessment updatedAssessment) {
        Assessment existing = getById(id);

        existing.setTitle(updatedAssessment.getTitle());
        existing.setCourseName(updatedAssessment.getCourseName());
        existing.setType(updatedAssessment.getType());
        existing.setStatus(updatedAssessment.getStatus());
        existing.setClassName(updatedAssessment.getClassName());
        existing.setStartDate(updatedAssessment.getStartDate());
        existing.setEndDate(updatedAssessment.getEndDate());
        existing.setDuration(updatedAssessment.getDuration());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}