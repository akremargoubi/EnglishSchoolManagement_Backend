package com.englishschool.enrollmentservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for course events from course-service.
 * Handles: course.deleted, course.unpublished, etc.
 * Only active when 'kafka' profile is enabled.
 */
@Component
@Profile("kafka")
@RequiredArgsConstructor
@Slf4j
public class CourseEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.course-events:course-events}", groupId = "enrollment-service")
    public void consume(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.has("eventType") ? node.get("eventType").asText() : "unknown";
            Long courseId = node.has("courseId") ? node.get("courseId").asLong() : null;

            log.info("Received course event: {} for course {}", eventType, courseId);

            switch (eventType) {
                case "COURSE_DELETED" -> handleCourseDeleted(courseId);
                case "COURSE_UNPUBLISHED" -> handleCourseUnpublished(courseId);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing course event: {}", e.getMessage());
        }
    }

    private void handleCourseDeleted(Long courseId) {
        if (courseId == null) return;
        log.info("Would cancel enrollments for deleted course: {}", courseId);
    }

    private void handleCourseUnpublished(Long courseId) {
        if (courseId == null) return;
        log.info("Course unpublished: {}", courseId);
    }
}
