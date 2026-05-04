package com.esprit.notificationms.controller;

import com.esprit.notificationms.dto.EmailRequest;
import com.esprit.notificationms.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(
            @Valid @RequestBody EmailRequest request,
            @RequestHeader(value = "X-Service-Origin", required = false) String serviceOrigin) {
        emailService.sendEmail(request, serviceOrigin);
        return ResponseEntity.ok("Email sent successfully");
    }
}
