package com.englishschool.schedule.service;

import com.englishschool.schedule.entity.Schedule;
import com.englishschool.schedule.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    // Create
    public Schedule createSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    // Read all
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    // Read one
    public Schedule getScheduleById(Long id) {
        return scheduleRepository.findById(id).orElse(null);
    }

    // Update
    public Schedule updateSchedule(Long id, Schedule scheduleDetails) {
        Schedule schedule = scheduleRepository.findById(id).orElse(null);
        if (schedule != null) {
            schedule.setDayOfWeek(scheduleDetails.getDayOfWeek());
            schedule.setStartTime(scheduleDetails.getStartTime());
            schedule.setEndTime(scheduleDetails.getEndTime());
            schedule.setRoom(scheduleDetails.getRoom());
            if (scheduleDetails.getCourseId() != null) schedule.setCourseId(scheduleDetails.getCourseId());
            if (scheduleDetails.getClassName() != null) schedule.setClassName(scheduleDetails.getClassName());
            return scheduleRepository.save(schedule);
        }
        return null;
    }

    // Delete
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    public List<Schedule> getByCourseId(Long courseId) {
        return scheduleRepository.findByCourseId(courseId);
    }

    public List<Schedule> getByClassName(String className) {
        return scheduleRepository.findByClassNameContainingIgnoreCase(className);
    }
}