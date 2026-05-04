package com.example.assessmentservice.controller;

import com.example.assessmentservice.entity.Notification;
import com.example.assessmentservice.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping
    public ResponseEntity<List<Notification>> getAll(HttpServletRequest req) {
        return ResponseEntity.ok(notificationService.getAll(callerEmail(req)));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(HttpServletRequest req) {
        return ResponseEntity.ok(notificationService.getUnread(callerEmail(req)));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest req) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(callerEmail(req))));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest req) {
        notificationService.markAllAsRead(callerEmail(req));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // /test endpoint deliberately removed (was a dev-only endpoint exposing spam risk)

    private String callerEmail(HttpServletRequest req) {
        return (String) req.getAttribute("callerEmail");
    }
}
