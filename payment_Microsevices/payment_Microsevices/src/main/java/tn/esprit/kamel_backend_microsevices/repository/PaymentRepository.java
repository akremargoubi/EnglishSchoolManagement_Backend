package tn.esprit.kamel_backend_microsevices.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.kamel_backend_microsevices.entitiy.PaymentEntity;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByStudentId(Long studentId);
    List<PaymentEntity> findByCourseId(Long courseId);
}