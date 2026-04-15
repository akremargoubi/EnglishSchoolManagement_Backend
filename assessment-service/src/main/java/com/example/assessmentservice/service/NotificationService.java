package com.example.assessmentservice.service;

import com.example.assessmentservice.entity.Notification;
import com.example.assessmentservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    // ── In-app notifications ───────────────────────────────────────────────────

    public List<Notification> getAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Notification> getUnread() {
        return notificationRepository.findByReadFalseOrderByCreatedAtDesc();
    }

    public long countUnread() {
        return notificationRepository.countByReadFalse();
    }

    public Notification markAsRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
        n.setRead(true);
        return notificationRepository.save(n);
    }

    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }

    // ── Assessment created ─────────────────────────────────────────────────────
    @Async
    public void notifyAssessmentCreated(String assessmentTitle, String courseName,
                                        String type, Long assessmentId,
                                        String recipientEmail) {
        // Save in-app notification
        Notification notification = Notification.builder()
                .type("ASSESSMENT_CREATED")
                .title("New Assessment: " + assessmentTitle)
                .message("A new " + type + " has been scheduled for course \"" + courseName + "\".")
                .targetEmail(recipientEmail)
                .relatedId(assessmentId)
                .read(false)
                .build();
        notificationRepository.save(notification);

        // Send email
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            sendEmail(
                    recipientEmail,
                    "📝 New Assessment: " + assessmentTitle,
                    buildAssessmentEmail(assessmentTitle, courseName, type)
            );
        }
    }

    // ── Resource added ─────────────────────────────────────────────────────────
    @Async
    public void notifyResourceAdded(String resourceTitle, String assessmentTitle,
                                    Long assessmentId, String recipientEmail) {
        // Save in-app notification
        Notification notification = Notification.builder()
                .type("RESOURCE_ADDED")
                .title("New Resource: " + resourceTitle)
                .message("A new resource \"" + resourceTitle + "\" has been added to \"" + assessmentTitle + "\".")
                .targetEmail(recipientEmail)
                .relatedId(assessmentId)
                .read(false)
                .build();
        notificationRepository.save(notification);

        // Send email
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            sendEmail(
                    recipientEmail,
                    "📁 New Resource Available: " + resourceTitle,
                    buildResourceEmail(resourceTitle, assessmentTitle)
            );
        }
    }

    // ── Email sender ───────────────────────────────────────────────────────────
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@englishschool.com");
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ── Email templates ────────────────────────────────────────────────────────
    private String buildAssessmentEmail(String title, String course, String type) {
        return """
                Hello,

                A new assessment has been scheduled for you.

                ─────────────────────────────
                📝 Title   : %s
                📚 Course  : %s
                🏷️  Type    : %s
                ─────────────────────────────

                Please log in to the platform to view the details and linked resources.

                English School Management Platform
                """.formatted(title, course, type);
    }

    private String buildResourceEmail(String resourceTitle, String assessmentTitle) {
        return """
                Hello,

                A new resource has been added to one of your assessments.

                ─────────────────────────────
                📁 Resource   : %s
                📝 Assessment : %s
                ─────────────────────────────

                Please log in to the platform to download the resource.

                English School Management Platform
                """.formatted(resourceTitle, assessmentTitle);
    }
}