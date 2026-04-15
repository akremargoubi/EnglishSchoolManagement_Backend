package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final AssessmentService service;

    @GetMapping("/calendar")
    public List<Assessment> getCalendar(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month
    ) {
        if (year == 0) year   = LocalDateTime.now().getYear();
        if (month == 0) month = LocalDateTime.now().getMonthValue();
        return service.getByMonth(year, month);
    }

    @GetMapping("/upcoming")
    public List<Assessment> getUpcoming() {
        return service.getUpcoming();
    }

    @GetMapping("/ongoing")
    public List<Assessment> getOngoing() {
        return service.getOngoing();
    }

    @GetMapping("/range")
    public List<Assessment> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return service.getByDateRange(start, end);
    }
}