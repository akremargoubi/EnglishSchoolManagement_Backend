package com.englishschool.attendance.service;

import com.englishschool.attendance.entity.Attendance;
import com.englishschool.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    // Create
    public Attendance createAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    // Read all
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    // Read one
    public Attendance getAttendanceById(Long id) {
        return attendanceRepository.findById(id).orElse(null);
    }

    // Update
    public Attendance updateAttendance(Long id, Attendance attendanceDetails) {
        Attendance attendance = attendanceRepository.findById(id).orElse(null);
        if (attendance != null) {
            attendance.setDate(attendanceDetails.getDate());
            attendance.setStatus(attendanceDetails.getStatus());
            return attendanceRepository.save(attendance);
        }
        return null;
    }

    // Delete
    public void deleteAttendance(Long id) {
        attendanceRepository.deleteById(id);
    }
}
