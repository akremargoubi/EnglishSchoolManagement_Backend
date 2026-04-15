package com.englishschool.courseservice.repository;

import com.englishschool.courseservice.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
}
