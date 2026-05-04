package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.AssessmentStatus;
import com.example.assessmentservice.entity.AssessmentType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/enums")
public class EnumController {

    @GetMapping("/types")
    public List<AssessmentType> getTypes() {
        return Arrays.asList(AssessmentType.values());
    }

    @GetMapping("/statuses")
    public List<AssessmentStatus> getStatuses() {
        return Arrays.asList(AssessmentStatus.values());
    }
}