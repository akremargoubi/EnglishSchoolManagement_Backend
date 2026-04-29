package com.englishschool.resourcesservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${assessment.service.url}")
    private String assessmentServiceUrl;

    private HttpEntity<?> authEntity() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpHeaders headers = new HttpHeaders();
        if (attrs != null) {
            String token = (String) attrs.getRequest().getAttribute("callerToken");
            if (token != null) headers.setBearerAuth(token);
        }
        return new HttpEntity<>(headers);
    }

    /** Returns className of a student's enrolled class, or null. */
    public String getStudentClassName(UUID userId) {
        try {
            Map<?, ?> user = restTemplate.exchange(
                    authServiceUrl + "/api/users/" + userId,
                    HttpMethod.GET, authEntity(), Map.class).getBody();
            return user == null ? null : (String) user.get("className");
        } catch (Exception e) {
            log.warn("getStudentClassName failed for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /** Returns list of class names the tutor teaches. */
    @SuppressWarnings("unchecked")
    public List<String> getTutorClassNames(UUID tutorId) {
        try {
            List<?> classes = restTemplate.exchange(
                    authServiceUrl + "/api/classes/by-tutor/" + tutorId,
                    HttpMethod.GET, authEntity(), List.class).getBody();
            if (classes == null) return List.of();
            return classes.stream()
                    .filter(c -> c instanceof Map)
                    .map(c -> (String) ((Map<?, ?>) c).get("name"))
                    .filter(n -> n != null)
                    .toList();
        } catch (Exception e) {
            log.warn("getTutorClassNames failed for {}: {}", tutorId, e.getMessage());
            return List.of();
        }
    }

    /** Returns the className of the assessment, or null. */
    public String getAssessmentClassName(Long assessmentId) {
        try {
            Map<?, ?> assessment = restTemplate.exchange(
                    assessmentServiceUrl + "/api/assessments/" + assessmentId,
                    HttpMethod.GET, authEntity(), Map.class).getBody();
            return assessment == null ? null : (String) assessment.get("className");
        } catch (Exception e) {
            log.warn("getAssessmentClassName failed for {}: {}", assessmentId, e.getMessage());
            return null;
        }
    }

    public boolean isTutorOfClass(UUID userId, String className) {
        if (className == null) return false;
        return getTutorClassNames(userId).stream().anyMatch(n -> n.equalsIgnoreCase(className));
    }

    public boolean isStudentInClass(UUID userId, String className) {
        if (className == null) return false;
        return className.equalsIgnoreCase(getStudentClassName(userId));
    }
}
