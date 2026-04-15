package tn.esprit.kamel_backend_microsevices.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.esprit.kamel_backend_microsevices.entitiy.PaymentEntity;
import tn.esprit.kamel_backend_microsevices.service.EmailService;
import tn.esprit.kamel_backend_microsevices.service.PaymentService;


@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")

public class PaymentController {
    private static final String DEFAULT_RECEIVER = "kamel.hamdi@esprit.tn";

    private final PaymentService service;
    private final EmailService emailService;

    public PaymentController(PaymentService service, EmailService emailService) {
        this.service = service;
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<PaymentEntity> create(@RequestBody PaymentEntity entity) {
        return ResponseEntity.ok(service.create(entity));
    }

    @GetMapping
    public ResponseEntity<List<PaymentEntity>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<PaymentEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentEntity> update(@PathVariable Long id, @RequestBody PaymentEntity payload) {
        return ResponseEntity.ok(service.update(id, payload));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-student/{studentId}")
    public ResponseEntity<List<PaymentEntity>> byStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(service.findByStudent(studentId));
    }

    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail() {
        emailService.sendEmail(
                DEFAULT_RECEIVER,
                "SMTP Test - Payment Service",
                "This is a test email from payment service configuration."
        );
        return ResponseEntity.ok("Test email sent");
    }
}