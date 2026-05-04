package com.example.assessmentservice.repository;

import com.example.assessmentservice.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findByClassName(String className);

    List<Assessment> findByClassNameIn(List<String> classNames);

    List<Assessment> findByTutorId(UUID tutorId);

    // Planning queries — dates stored as ISO strings (e.g. "2026-04-24")
    @Query("SELECT a FROM Assessment a WHERE a.startDate LIKE :prefix%")
    List<Assessment> findByYearMonth(@Param("prefix") String prefix);

    @Query("SELECT a FROM Assessment a WHERE a.startDate > :now ORDER BY a.startDate ASC")
    List<Assessment> findUpcoming(@Param("now") String now);

    @Query("SELECT a FROM Assessment a WHERE a.startDate <= :now AND a.endDate >= :now")
    List<Assessment> findOngoing(@Param("now") String now);

    @Query("SELECT a FROM Assessment a WHERE a.startDate >= :start AND a.startDate <= :end ORDER BY a.startDate ASC")
    List<Assessment> findByDateRange(@Param("start") String start, @Param("end") String end);
}
