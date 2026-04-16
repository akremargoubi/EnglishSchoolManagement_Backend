// src/main/java/com/esprit/esmauthms/service/EmailClient.java
package com.esprit.esmauthms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${email.service.url:http://localhost:2001/api/emails/send}")
    private String emailServiceUrl;

    public void sendEmail(String to, String subject, String text) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("to", to);
            body.put("subject", subject);
            body.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                emailServiceUrl, HttpMethod.POST, request, String.class);

            log.info("Email service response: status={}, body={}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Failed to send email via email microservice", e);
        }
    }
}