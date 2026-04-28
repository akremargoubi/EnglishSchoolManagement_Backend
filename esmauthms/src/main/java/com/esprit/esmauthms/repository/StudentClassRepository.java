package com.esprit.esmauthms.repository;

import com.esprit.esmauthms.entity.StudentClass;
import com.esprit.esmauthms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {
    Optional<StudentClass> findByName(String name);
    List<StudentClass> findByTutor_Id(UUID tutorId);
}