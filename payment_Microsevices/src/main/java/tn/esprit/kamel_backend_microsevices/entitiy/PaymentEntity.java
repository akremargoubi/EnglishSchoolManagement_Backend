package tn.esprit.kamel_backend_microsevices.entitiy;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class PaymentEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Double amount;
    private String method;   // CARD / CASH / TRANSFER...
    private String status;   // PENDING / PAID / FAILED...
    private LocalDateTime date;

    // روابط اختيارية (حسب ال domain mte3ek)
    private Long studentId;
    private Long courseId;
    private Long enrollmentId;

    @PrePersist
    public void onCreate() {
        if (date == null) date = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }

    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public void setAmount(Double amount) { this.amount = amount; }

    public void setMethod(String method) { this.method = method; }

    public void setStatus(String status) { this.status = status; }

    public void setDate(LocalDateTime date) { this.date = date; }

    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }
}

