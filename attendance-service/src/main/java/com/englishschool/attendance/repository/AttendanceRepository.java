package com.englishschool.attendance.repository;

import com.englishschool.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudentEmail(String studentEmail);
    List<Attendance> findByCourseId(Long courseId);
    List<Attendance> findByStudentEmailAndCourseId(String studentEmail, Long courseId);
}
