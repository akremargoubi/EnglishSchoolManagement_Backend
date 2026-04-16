package tn.esprit.kamel_backend_microsevices.service;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.kamel_backend_microsevices.entitiy.PaymentEntity;
import tn.esprit.kamel_backend_microsevices.repository.PaymentRepository;


@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final EmailService emailService;

    public PaymentService(PaymentRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Transactional
    public PaymentEntity create(PaymentEntity p) {
        p.setPaymentId(null); // safety
        PaymentEntity saved = repository.save(p);

        String clientEmail = "kamel.hamdi@esprit.tn";
        String subject = "New Payment Created";
        String body = "Dear Client,\n\n"
                + "Thank you for your payment. A new payment has been created with the following details:\n\n"
                + "Payment ID: " + saved.getPaymentId() + "\n"
                + "Amount: " + saved.getAmount() + "\n"
                + "Method: " + saved.getMethod() + "\n"
                + "Status: " + saved.getStatus() + "\n"
                + "Date: " + saved.getDate() + "\n"
                + "Student ID: " + saved.getStudentId() + "\n"
                + "Course ID: " + saved.getCourseId() + "\n"
                + "Enrollment ID: " + saved.getEnrollmentId() + "\n\n"
                + "We appreciate your trust in our platform. If you have any questions, "
                + "please contact our support team.\n\n"
                + "Best regards,\n"
                + "Payment Support Team";
        try {
            emailService.sendEmail(clientEmail, subject, body);
        } catch (Exception e) {
            logger.error("Payment cancelled because email sending failed for paymentId={}", saved.getPaymentId(), e);
            }
        return saved;
    }

    public List<PaymentEntity> findAll() {
        return repository.findAll();
    }

    public PaymentEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    public PaymentEntity update(Long id, PaymentEntity payload) {
        PaymentEntity existing = findById(id);
        existing.setAmount(payload.getAmount());
        existing.setMethod(payload.getMethod());
        existing.setStatus(payload.getStatus());
        existing.setDate(payload.getDate());
        existing.setStudentId(payload.getStudentId());
        existing.setCourseId(payload.getCourseId());
        existing.setEnrollmentId(payload.getEnrollmentId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<PaymentEntity> findByStudent(Long studentId) {
        return repository.findByStudentId(studentId);
    }
}