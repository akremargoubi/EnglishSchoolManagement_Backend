package com.englishschool.courseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import com.englishschool.courseservice.entity.Course;
import com.englishschool.courseservice.repository.CourseRepository;

@SpringBootApplication
public class CourseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }

    /**
     * sample data loader - executed on startup. the H2 in‑memory database is empty
     * every time the application restarts, so we insert a few courses here.
     */
    @Bean
    public CommandLineRunner loadSampleData(CourseRepository repository) {
        return args -> {
            // clear existing data just in case
            repository.deleteAll();
            repository.save(new Course(null, "Beginner English", "A1", "Introduction to English"));
            repository.save(new Course(null, "Intermediate English", "B1", "Conversation and grammar"));
            repository.save(new Course(null, "Advanced English", "C1", "Academic writing"));
        };
    }

}
