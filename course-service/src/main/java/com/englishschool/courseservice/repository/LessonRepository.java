package com.englishschool.courseservice.repository;

import com.englishschool.courseservice.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByModule_IdOrderByOrderIndexAsc(Long moduleId);
}
