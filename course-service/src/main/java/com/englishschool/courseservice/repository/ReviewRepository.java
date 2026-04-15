package com.englishschool.courseservice.repository;

import com.englishschool.courseservice.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByCourseCourseId(Long courseId, Pageable pageable);
    List<Review> findByCourseCourseId(Long courseId);
    boolean existsByCourseCourseIdAndUserId(Long courseId, Long userId);
}
