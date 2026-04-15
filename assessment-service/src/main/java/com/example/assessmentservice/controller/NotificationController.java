package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.Notification;
import com.example.assessmentservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET all notifications
    @GetMapping
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    // GET unread notifications
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread() {
        return ResponseEntity.ok(notificationService.getUnread());
    }

    // GET unread count
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread()));
    }

    // PUT mark one as read
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // PUT mark all as read
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    // DELETE notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST test notification (for dev)
    @PostMapping("/test")
    public ResponseEntity<String> testNotification(@RequestParam String email) {
        notificationService.notifyAssessmentCreated(
                "Test Assessment", "English B2", "EXAM", 1L, email
        );
        return ResponseEntity.ok("Notification sent to " + email);
    }
}