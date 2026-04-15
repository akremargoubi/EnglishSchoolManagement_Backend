package com.example.assessmentservice.repository;

import com.example.assessmentservice.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByAssessmentId(Long assessmentId);

    List<Grade> findByStudentEmail(String studentEmail);

    boolean existsByAssessmentIdAndStudentEmail(Long assessmentId, String studentEmail);

    @Query("SELECT AVG(g.score) FROM Grade g WHERE g.assessmentId = :assessmentId")
    Double getAverageByAssessment(@Param("assessmentId") Long assessmentId);

    @Query("SELECT MAX(g.score) FROM Grade g WHERE g.assessmentId = :assessmentId")
    Double getMaxByAssessment(@Param("assessmentId") Long assessmentId);

    @Query("SELECT MIN(g.score) FROM Grade g WHERE g.assessmentId = :assessmentId")
    Double getMinByAssessment(@Param("assessmentId") Long assessmentId);

    @Query("SELECT COUNT(g) FROM Grade g WHERE g.assessmentId = :assessmentId AND g.score >= (g.maxScore * 0.6)")
    Long countPassing(@Param("assessmentId") Long assessmentId);
}