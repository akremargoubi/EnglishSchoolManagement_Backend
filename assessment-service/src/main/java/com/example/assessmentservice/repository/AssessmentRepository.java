package com.example.assessmentservice.repository;

import com.example.assessmentservice.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    // Filtre par classe — utilisé pour le dashboard étudiant
    List<Assessment> findByClassName(String className);
}