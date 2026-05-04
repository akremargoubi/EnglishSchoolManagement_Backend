package com.esprit.notificationms.controller;

import com.esprit.notificationms.entity.EmailLog;
import com.esprit.notificationms.enums.EmailStatus;
import com.esprit.notificationms.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/emails/logs")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogRepository emailLogRepository;

    @GetMapping
    public ResponseEntity<List<EmailLog>> getAllLogs(
            @RequestParam(required = false) EmailStatus status,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String serviceOrigin) {

        List<EmailLog> logs;

        if (status != null) {
            logs = emailLogRepository.findByStatus(status);
        } else if (recipient != null) {
            logs = emailLogRepository.findByRecipient(recipient);
        } else if (serviceOrigin != null) {
            logs = emailLogRepository.findByServiceOrigin(serviceOrigin);
        } else {
            logs = emailLogRepository.findAll();
        }

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailLog> getLogById(@PathVariable Long id) {
        EmailLog log = emailLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email log not found with id: " + id));
        return ResponseEntity.ok(log);
    }
}
