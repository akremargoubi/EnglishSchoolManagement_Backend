package com.englishschool.enrollmentservice.repository;

import com.englishschool.enrollmentservice.entity.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {
    Optional<StudentProgress> findByEnrollment_Id(Long enrollmentId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StudentProgress sp WHERE sp.enrollment.id = :enrollmentId")
    void deleteByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
