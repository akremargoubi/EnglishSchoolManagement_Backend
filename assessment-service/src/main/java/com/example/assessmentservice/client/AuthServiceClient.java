package com.example.assessmentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calls esmauthms to resolve class-membership and tutor-ownership.
 * All calls are fail-safe: on error they return false/null (deny by default).
 */
@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth.service.url}")
    private String authServiceUrl;

    /** Returns the className of the student's enrolled class, or null. */
    public String getStudentClassName(UUID userId) {
        try {
            Map<?, ?> user = restTemplate.getForObject(
                    authServiceUrl + "/api/users/" + userId, Map.class);
            if (user == null) return null;
            return (String) user.get("className");
        } catch (Exception e) {
            log.warn("AuthServiceClient.getStudentClassName failed for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /** Returns the list of class names this tutor is assigned to. */
    @SuppressWarnings("unchecked")
    public List<String> getTutorClassNames(UUID tutorId) {
        try {
            List<?> classes = restTemplate.getForObject(
                    authServiceUrl + "/api/classes/by-tutor/" + tutorId, List.class);
            if (classes == null) return List.of();
            return classes.stream()
                    .filter(c -> c instanceof Map)
                    .map(c -> (String) ((Map<?, ?>) c).get("name"))
                    .filter(n -> n != null)
                    .toList();
        } catch (Exception e) {
            log.warn("AuthServiceClient.getTutorClassNames failed for {}: {}", tutorId, e.getMessage());
            return List.of();
        }
    }

    public boolean isTutorOfClass(UUID userId, String className) {
        if (className == null) return false;
        return getTutorClassNames(userId).stream()
                .anyMatch(name -> name.equalsIgnoreCase(className));
    }

    public boolean isStudentInClass(UUID userId, String className) {
        if (className == null) return false;
        String enrolled = getStudentClassName(userId);
        return className.equalsIgnoreCase(enrolled);
    }
}
