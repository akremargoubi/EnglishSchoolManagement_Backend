package com.englishschool.enrollmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "course-service", url = "${course.service.url:http://localhost:8083}")
public interface CourseFeignClient {

    @GetMapping("/api/v1/courses/{id}")
    CourseDTO getCourseById(@PathVariable("id") Long id);

    @GetMapping(value = "/api/v1/modules", params = "courseId")
    List<ModuleDTO> getModulesByCourseId(@RequestParam("courseId") Long courseId,
                                         @RequestParam(value = "includeLessons", defaultValue = "true") boolean includeLessons);
}
