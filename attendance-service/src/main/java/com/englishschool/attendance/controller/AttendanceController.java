package com.englishschool.attendance.controller;

import com.englishschool.attendance.entity.Attendance;
import com.englishschool.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceService.getAllAttendances();
    }

    @GetMapping("/{id}")
    public Attendance getAttendanceById(@PathVariable Long id) {
        return attendanceService.getAttendanceById(id);
    }

    @PostMapping
    public Attendance createAttendance(@RequestBody Attendance attendance) {
        return attendanceService.createAttendance(attendance);
    }

    @PutMapping("/{id}")
    public Attendance updateAttendance(@PathVariable Long id, @RequestBody Attendance attendance) {
        return attendanceService.updateAttendance(id, attendance);
    }

    @DeleteMapping("/{id}")
    public void deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
    }

    @GetMapping("/student/{email}")
    public List<Attendance> getByStudentEmail(@PathVariable String email) {
        return attendanceService.getByStudentEmail(email);
    }

    @GetMapping("/course/{courseId}")
    public List<Attendance> getByCourse(@PathVariable Long courseId) {
        return attendanceService.getByCourseId(courseId);
    }

    @GetMapping("/student/{email}/course/{courseId}")
    public List<Attendance> getByStudentAndCourse(@PathVariable String email, @PathVariable Long courseId) {
        return attendanceService.getByStudentEmailAndCourseId(email, courseId);
    }
}
