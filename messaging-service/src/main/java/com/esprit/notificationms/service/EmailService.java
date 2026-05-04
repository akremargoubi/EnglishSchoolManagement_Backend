package com.esprit.notificationms.service;

import com.esprit.notificationms.dto.EmailRequest;
import com.esprit.notificationms.entity.EmailLog;
import com.esprit.notificationms.enums.EmailStatus;
import com.esprit.notificationms.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${mailjet.api-key}")
    private String apiKey;

    @Value("${mailjet.secret-key}")
    private String secretKey;

    @Value("${mailjet.sender-email}")
    private String senderEmail;

    @Value("${mailjet.sender-name}")
    private String senderName;

    private final EmailLogRepository emailLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendEmail(EmailRequest request) {
        sendEmail(request, null);
    }

    public void sendEmail(EmailRequest request, String serviceOrigin) {
        EmailLog log = EmailLog.builder()
                .recipient(request.getTo())
                .subject(request.getSubject())
                .serviceOrigin(serviceOrigin)
                .sentAt(LocalDateTime.now())
                .build();

        try {
            String url = "https://api.mailjet.com/v3.1/send";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(apiKey, secretKey);

            Map<String, Object> body = Map.of(
                    "Messages", new Object[]{
                            Map.of(
                                    "From", Map.of(
                                            "Email", senderEmail,
                                            "Name", senderName
                                    ),
                                    "To", new Object[]{
                                            Map.of(
                                                    "Email", request.getTo()
                                            )
                                    },
                                    "Subject", request.getSubject(),
                                    "TextPart", request.getText()
                            )
                    }
            );

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);

            log.setStatus(EmailStatus.SENT);

        } catch (RestClientException e) {
            log.setStatus(EmailStatus.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        emailLogRepository.save(log);
    }
}
