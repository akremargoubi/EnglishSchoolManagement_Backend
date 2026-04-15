package com.englishschool.attendance.repository;

import com.englishschool.attendance.entity.Attendance;
import com.englishschool.attendance.entity.AttendanceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AttendanceRepositoryTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Test
    void testSaveAttendance() {
        // Arrange
        Attendance attendance = new Attendance();
        attendance.setDate(LocalDate.now());
        attendance.setStatus(AttendanceStatus.PRESENT);

        // Act
        Attendance saved = attendanceRepository.save(attendance);

        // Assert
        assertNotNull(saved.getAttended());
        assertEquals(AttendanceStatus.PRESENT, saved.getStatus());
    }

    @Test
    void testFindAllAttendances() {
        // Arrange
        attendanceRepository.save(createAttendance(AttendanceStatus.PRESENT));
        attendanceRepository.save(createAttendance(AttendanceStatus.LATE));

        // Act
        List<Attendance> result = attendanceRepository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    private Attendance createAttendance(AttendanceStatus status) {
        Attendance a = new Attendance();
        a.setDate(LocalDate.now());
        a.setStatus(status);
        return a;
    }
}
