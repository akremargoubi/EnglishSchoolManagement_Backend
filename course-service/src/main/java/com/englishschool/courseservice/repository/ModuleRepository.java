package com.englishschool.courseservice.repository;

import com.englishschool.courseservice.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByCourseCourseIdOrderByOrderIndexAsc(Long courseId);
}
