package com.esprit.notificationms.controller;

import com.esprit.notificationms.entity.ScheduledEmail;
import com.esprit.notificationms.enums.EmailStatus;
import com.esprit.notificationms.service.ScheduledEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/emails/scheduled")
@RequiredArgsConstructor
public class ScheduledEmailController {

    private final ScheduledEmailService scheduledEmailService;

    @PostMapping
    public ResponseEntity<ScheduledEmail> scheduleEmail(@RequestBody ScheduledEmail scheduledEmail) {
        ScheduledEmail saved = scheduledEmailService.scheduleEmail(scheduledEmail);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<ScheduledEmail>> getAllScheduledEmails(
            @RequestParam(required = false) EmailStatus status) {
        List<ScheduledEmail> emails;
        if (status != null) {
            emails = scheduledEmailService.findByStatus(status);
        } else {
            emails = scheduledEmailService.getAll();
        }
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledEmail> getScheduledEmail(@PathVariable Long id) {
        ScheduledEmail email = scheduledEmailService.getById(id);
        return ResponseEntity.ok(email);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ScheduledEmail> cancelEmail(@PathVariable Long id) {
        ScheduledEmail cancelled = scheduledEmailService.cancelScheduledEmail(id);
        return ResponseEntity.ok(cancelled);
    }
}
