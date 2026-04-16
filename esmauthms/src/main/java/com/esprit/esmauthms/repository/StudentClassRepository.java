package com.esprit.esmauthms.repository;

import com.esprit.esmauthms.entity.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {
    Optional<StudentClass> findByName(String name);
}