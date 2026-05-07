package com.englishschool.schedule;

import com.englishschool.schedule.controller.ScheduleController;
import com.englishschool.schedule.entity.Schedule;
import com.englishschool.schedule.service.ScheduleService;
import com.englishschool.schedule.service.WeatherService;
import com.englishschool.schedule.service.WeatherData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private ScheduleController scheduleController;

    // ========== SCHEDULE TESTS ==========

    @Test
    void testGetAllSchedules() {
        // Arrange
        Schedule schedule1 = new Schedule("Monday", LocalTime.of(9, 0), LocalTime.of(10, 30), "A101");
        Schedule schedule2 = new Schedule("Wednesday", LocalTime.of(14, 0), LocalTime.of(15, 30), "B202");
        List<Schedule> mockSchedules = Arrays.asList(schedule1, schedule2);

        when(scheduleService.getAllSchedules()).thenReturn(mockSchedules);

        // Act
        List<Schedule> result = scheduleController.getAllSchedules();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Monday", result.get(0).getDayOfWeek());
        verify(scheduleService, times(1)).getAllSchedules();
    }

    @Test
    void testGetScheduleById() {
        // Arrange
        Long id = 1L;
        Schedule mockSchedule = new Schedule("Tuesday", LocalTime.of(10, 0), LocalTime.of(11, 30), "C303");
        when(scheduleService.getScheduleById(id)).thenReturn(mockSchedule);

        // Act
        Schedule result = scheduleController.getScheduleById(id);

        // Assert
        assertNotNull(result);
        assertEquals("Tuesday", result.getDayOfWeek());
        assertEquals("C303", result.getRoom());
        verify(scheduleService, times(1)).getScheduleById(id);
    }

    @Test
    void testCreateSchedule() {
        // Arrange
        Schedule newSchedule = new Schedule("Friday", LocalTime.of(13, 0), LocalTime.of(14, 30), "D404");
        when(scheduleService.createSchedule(any(Schedule.class))).thenReturn(newSchedule);

        // Act
        Schedule result = scheduleController.createSchedule(newSchedule);

        // Assert
        assertNotNull(result);
        assertEquals("Friday", result.getDayOfWeek());
        assertEquals("D404", result.getRoom());
        verify(scheduleService, times(1)).createSchedule(any(Schedule.class));
    }

    @Test
    void testUpdateSchedule() {
        // Arrange
        Long id = 1L;
        Schedule updatedSchedule = new Schedule("Thursday", LocalTime.of(15, 0), LocalTime.of(16, 30), "E505");
        when(scheduleService.updateSchedule(eq(id), any(Schedule.class))).thenReturn(updatedSchedule);

        // Act
        Schedule result = scheduleController.updateSchedule(id, updatedSchedule);

        // Assert
        assertNotNull(result);
        assertEquals("Thursday", result.getDayOfWeek());
        verify(scheduleService, times(1)).updateSchedule(eq(id), any(Schedule.class));
    }

    @Test
    void testDeleteSchedule() {
        // Arrange
        Long id = 1L;
        doNothing().when(scheduleService).deleteSchedule(id);

        // Act
        scheduleController.deleteSchedule(id);

        // Assert
        verify(scheduleService, times(1)).deleteSchedule(id);
    }

    // ========== WEATHER TESTS ==========

    @Test
    void testGetWeather() {
        // Arrange
        String dayOfWeek = "Monday";
        String date = "2026-04-29";
        LocalDate localDate = LocalDate.parse(date);
        WeatherData mockWeather = new WeatherData(24.5, 18.0, 10, 0, "☀️ Clear Sky");

        when(weatherService.getWeatherForDay(dayOfWeek, localDate)).thenReturn(mockWeather);

        // Act
        ResponseEntity<WeatherData> response = scheduleController.getWeather(dayOfWeek, date);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(24.5, response.getBody().temperature());
        verify(weatherService, times(1)).getWeatherForDay(dayOfWeek, localDate);
    }

    @Test
    void testGetWeatherWithRain() {
        // Arrange
        String dayOfWeek = "Wednesday";
        String date = "2026-04-30";
        LocalDate localDate = LocalDate.parse(date);
        WeatherData mockWeather = new WeatherData(18.5, 15.0, 80, 61, "🌧️ Rainy");

        when(weatherService.getWeatherForDay(dayOfWeek, localDate)).thenReturn(mockWeather);

        // Act
        ResponseEntity<WeatherData> response = scheduleController.getWeather(dayOfWeek, date);

        // Assert
        assertNotNull(response);
        assertEquals(80, response.getBody().rainProbability());
        assertTrue(response.getBody().condition().contains("Rain"));
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void testGetScheduleByIdNotFound() {
        // Arrange
        Long id = 999L;
        when(scheduleService.getScheduleById(id)).thenReturn(null);

        // Act
        Schedule result = scheduleController.getScheduleById(id);

        // Assert
        assertNull(result);
        verify(scheduleService, times(1)).getScheduleById(id);
    }

    @Test
    void testGetAllSchedulesEmptyList() {
        // Arrange
        when(scheduleService.getAllSchedules()).thenReturn(Arrays.asList());

        // Act
        List<Schedule> result = scheduleController.getAllSchedules();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(scheduleService, times(1)).getAllSchedules();
    }

    @Test
    void testWeatherServiceHandlesException() {
        // Arrange
        String dayOfWeek = "Friday";
        String date = "2026-05-01";
        LocalDate localDate = LocalDate.parse(date);

        // Simulate weather service returning fallback data on error
        WeatherData fallbackWeather = new WeatherData(22.0, 18.0, 10, 1, "⛅ Partly Cloudy");
        when(weatherService.getWeatherForDay(dayOfWeek, localDate)).thenReturn(fallbackWeather);

        // Act
        ResponseEntity<WeatherData> response = scheduleController.getWeather(dayOfWeek, date);

        // Assert
        assertNotNull(response);
        assertEquals(22.0, response.getBody().temperature());
        assertTrue(response.getBody().condition().contains("Partly"));
    }
}