package com.esprit.notificationms.entity;

import com.esprit.notificationms.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    // which microservice triggered the send (optional header X-Service-Origin)
    private String serviceOrigin;

    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}