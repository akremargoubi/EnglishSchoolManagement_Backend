package com.englishschool.schedule.controller;

import com.englishschool.schedule.entity.Schedule;
import com.englishschool.schedule.service.ScheduleService;
import com.englishschool.schedule.service.WeatherService;
import com.englishschool.schedule.service.WeatherData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private WeatherService weatherService;  // ADD THIS LINE

    @GetMapping
    public List<Schedule> getAllSchedules() {
        return scheduleService.getAllSchedules();
    }

    @GetMapping("/{id}")
    public Schedule getScheduleById(@PathVariable Long id) {
        return scheduleService.getScheduleById(id);
    }

    @PostMapping
    public Schedule createSchedule(@RequestBody Schedule schedule) {
        return scheduleService.createSchedule(schedule);
    }

    @PutMapping("/{id}")
    public Schedule updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        return scheduleService.updateSchedule(id, schedule);
    }

    @DeleteMapping("/{id}")
    public void deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
    }

    @GetMapping("/weather/{dayOfWeek}/{date}")
    public ResponseEntity<WeatherData> getWeather(@PathVariable String dayOfWeek, @PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        WeatherData weather = weatherService.getWeatherForDay(dayOfWeek, localDate);
        return ResponseEntity.ok(weather);
    }
}