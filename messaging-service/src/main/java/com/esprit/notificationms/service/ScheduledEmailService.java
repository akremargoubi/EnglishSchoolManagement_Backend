package com.esprit.notificationms.service;

import com.esprit.notificationms.dto.EmailRequest;
import com.esprit.notificationms.entity.EmailLog;
import com.esprit.notificationms.entity.ScheduledEmail;
import com.esprit.notificationms.enums.EmailStatus;
import com.esprit.notificationms.repository.EmailLogRepository;
import com.esprit.notificationms.repository.ScheduledEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledEmailService {

    private final ScheduledEmailRepository scheduledEmailRepository;
    private final EmailLogRepository emailLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mailjet.api-key}")
    private String apiKey;

    @Value("${mailjet.secret-key}")
    private String secretKey;

    @Value("${mailjet.sender-email}")
    private String senderEmail;

    @Value("${mailjet.sender-name}")
    private String senderName;

    public ScheduledEmail scheduleEmail(ScheduledEmail scheduledEmail) {
        scheduledEmail.setStatus(EmailStatus.PENDING);
        scheduledEmail.setCreatedAt(LocalDateTime.now());
        return scheduledEmailRepository.save(scheduledEmail);
    }

    public ScheduledEmail cancelScheduledEmail(Long id) {
        ScheduledEmail scheduledEmail = scheduledEmailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scheduled email not found with id: " + id));

        if (scheduledEmail.getStatus() != EmailStatus.PENDING) {
            throw new RuntimeException("Can only cancel emails with PENDING status. Current status: " + scheduledEmail.getStatus());
        }

        scheduledEmail.setStatus(EmailStatus.CANCELLED);
        return scheduledEmailRepository.save(scheduledEmail);
    }

    public List<ScheduledEmail> getAll() {
        return scheduledEmailRepository.findAll();
    }

    public List<ScheduledEmail> findByStatus(EmailStatus status) {
        return scheduledEmailRepository.findByStatus(status);
    }

    public ScheduledEmail getById(Long id) {
        return scheduledEmailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scheduled email not found with id: " + id));
    }

    @Scheduled(fixedRate = 60000)
    public void processScheduledEmails() {
        log.info("Processing scheduled emails...");

        LocalDateTime now = LocalDateTime.now();
        List<ScheduledEmail> pendingEmails = scheduledEmailRepository
                .findByStatusAndScheduledAtBefore(EmailStatus.PENDING, now);

        log.info("Found {} pending emails to process", pendingEmails.size());

        for (ScheduledEmail scheduledEmail : pendingEmails) {
            try {
                sendScheduledEmail(scheduledEmail);
            } catch (Exception e) {
                log.error("Failed to process scheduled email {}: {}", scheduledEmail.getId(), e.getMessage());
            }
        }
    }

    private void sendScheduledEmail(ScheduledEmail scheduledEmail) {
        EmailRequest request = new EmailRequest();
        request.setTo(scheduledEmail.getRecipient());
        request.setSubject(scheduledEmail.getSubject());
        request.setText(scheduledEmail.getText());

        EmailLog emailLog = EmailLog.builder()
                .recipient(scheduledEmail.getRecipient())
                .subject(scheduledEmail.getSubject())
                .serviceOrigin(scheduledEmail.getServiceOrigin())
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
                                                    "Email", scheduledEmail.getRecipient()
                                            )
                                    },
                                    "Subject", scheduledEmail.getSubject(),
                                    "TextPart", scheduledEmail.getText()
                            )
                    }
            );

            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);

            emailLog.setStatus(EmailStatus.SENT);
            scheduledEmail.setStatus(EmailStatus.SENT);

        } catch (RestClientException e) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            scheduledEmail.setStatus(EmailStatus.FAILED);
        }

        scheduledEmail.setProcessedAt(LocalDateTime.now());
        scheduledEmailRepository.save(scheduledEmail);
        emailLogRepository.save(emailLog);

        log.info("Processed scheduled email {}: {}", scheduledEmail.getId(), emailLog.getStatus());
    }
}
