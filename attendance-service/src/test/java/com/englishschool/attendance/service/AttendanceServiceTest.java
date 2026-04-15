package com.englishschool.attendance.service;

import com.englishschool.attendance.entity.Attendance;
import com.englishschool.attendance.entity.AttendanceStatus;
import com.englishschool.attendance.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Attendance testAttendance;

    @BeforeEach
    void setUp() {
        testAttendance = new Attendance();
        testAttendance.setAttended(1L);
        testAttendance.setDate(LocalDate.now());
        testAttendance.setStatus(AttendanceStatus.PRESENT);
    }

    @Test
    void testCreateAttendance_Success() {
        // Arrange
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        // Act
        Attendance result = attendanceService.createAttendance(testAttendance);

        // Assert
        assertNotNull(result);
        assertEquals(AttendanceStatus.PRESENT, result.getStatus());
        verify(attendanceRepository, times(1)).save(testAttendance);
    }

    @Test
    void testGetAllAttendances_ReturnsList() {
        // Arrange
        List<Attendance> expectedList = Arrays.asList(testAttendance, new Attendance());
        when(attendanceRepository.findAll()).thenReturn(expectedList);

        // Act
        List<Attendance> result = attendanceService.getAllAttendances();

        // Assert
        assertEquals(2, result.size());
        verify(attendanceRepository, times(1)).findAll();
    }

    @Test
    void testGetAttendanceById_Found() {
        // Arrange
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(testAttendance));

        // Act
        Attendance result = attendanceService.getAttendanceById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getAttended());
    }

    @Test
    void testGetAttendanceById_NotFound() {
        // Arrange
        when(attendanceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Attendance result = attendanceService.getAttendanceById(999L);

        // Assert
        assertNull(result);
    }

    @Test
    void testUpdateAttendance_Success() {
        // Arrange
        Attendance updatedDetails = new Attendance();
        updatedDetails.setDate(LocalDate.of(2026, 4, 14));
        updatedDetails.setStatus(AttendanceStatus.LATE);

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(testAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(updatedDetails);

        // Act
        Attendance result = attendanceService.updateAttendance(1L, updatedDetails);

        // Assert
        assertNotNull(result);
        assertEquals(AttendanceStatus.LATE, result.getStatus());
    }

    @Test
    void testDeleteAttendance_Success() {
        // Arrange
        doNothing().when(attendanceRepository).deleteById(1L);

        // Act
        attendanceService.deleteAttendance(1L);

        // Assert
        verify(attendanceRepository, times(1)).deleteById(1L);
    }
}
