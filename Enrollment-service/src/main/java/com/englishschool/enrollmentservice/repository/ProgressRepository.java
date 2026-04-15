package com.englishschool.enrollmentservice.repository;

import com.englishschool.enrollmentservice.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, Long> {
    List<Progress> findByEnrollment_Id(Long enrollmentId);
    boolean existsByEnrollment_IdAndLessonId(Long enrollmentId, Long lessonId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Progress p WHERE p.enrollment.id = :enrollmentId")
    void deleteByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
