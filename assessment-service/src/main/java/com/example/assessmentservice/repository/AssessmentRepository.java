package com.example.assessmentservice.repository;

import com.example.assessmentservice.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    // Assessments d'un mois donné (pour le calendrier)
    @Query("SELECT a FROM Assessment a WHERE " +
            "a.startDate >= :start AND a.startDate < :end " +
            "ORDER BY a.startDate ASC")
    List<Assessment> findByMonth(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end
    );

    // Prochains assessments (upcoming)
    @Query("SELECT a FROM Assessment a WHERE " +
            "a.startDate > :now " +
            "ORDER BY a.startDate ASC")
    List<Assessment> findUpcoming(@Param("now") LocalDateTime now);

    // Assessments en cours
    @Query("SELECT a FROM Assessment a WHERE " +
            "a.startDate <= :now AND a.endDate >= :now " +
            "ORDER BY a.startDate ASC")
    List<Assessment> findOngoing(@Param("now") LocalDateTime now);

    // Assessments par plage de dates
    @Query("SELECT a FROM Assessment a WHERE " +
            "a.startDate >= :start AND a.endDate <= :end " +
            "ORDER BY a.startDate ASC")
    List<Assessment> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end")   LocalDateTime end
    );
}