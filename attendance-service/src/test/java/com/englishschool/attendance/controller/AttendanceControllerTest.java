package com.englishschool.attendance.controller;

import com.englishschool.attendance.entity.Attendance;
import com.englishschool.attendance.entity.AttendanceStatus;
import com.englishschool.attendance.service.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceService attendanceService;

    @Autowired
    private ObjectMapper objectMapper;

    private Attendance testAttendance;

    @BeforeEach
    void setUp() {
        testAttendance = new Attendance();
        testAttendance.setAttended(1L);
        testAttendance.setDate(LocalDate.now());
        testAttendance.setStatus(AttendanceStatus.PRESENT);
    }

    @Test
    void testGetAllAttendances() throws Exception {
        // Arrange
        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceService.getAllAttendances()).thenReturn(attendances);

        // Act & Assert
        mockMvc.perform(get("/api/attendances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attended").value(1L))
                .andExpect(jsonPath("$[0].status").value("PRESENT"));
    }

    @Test
    void testGetAttendanceById() throws Exception {
        // Arrange
        when(attendanceService.getAttendanceById(1L)).thenReturn(testAttendance);

        // Act & Assert
        mockMvc.perform(get("/api/attendances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attended").value(1L));
    }

    @Test
    void testCreateAttendance() throws Exception {
        // Arrange
        when(attendanceService.createAttendance(any(Attendance.class))).thenReturn(testAttendance);

        // Act & Assert
        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAttendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRESENT"));
    }

    @Test
    void testDeleteAttendance() throws Exception {
        // Arrange
        doNothing().when(attendanceService).deleteAttendance(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/attendances/1"))
                .andExpect(status().isOk());
    }
}

