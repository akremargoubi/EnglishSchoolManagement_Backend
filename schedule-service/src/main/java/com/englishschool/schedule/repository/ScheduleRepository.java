package com.englishschool.schedule.repository;

import com.englishschool.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByCourseId(Long courseId);
    List<Schedule> findByClassNameContainingIgnoreCase(String className);
}
