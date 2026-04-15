package com.example.assessmentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Enables @Async on NotificationService methods
    // Emails and notifications are sent in background threads
}