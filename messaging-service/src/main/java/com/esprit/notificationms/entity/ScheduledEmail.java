package com.esprit.notificationms.entity;

import com.esprit.notificationms.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_emails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status; // PENDING, SENT, FAILED, CANCELLED

    private String serviceOrigin;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}