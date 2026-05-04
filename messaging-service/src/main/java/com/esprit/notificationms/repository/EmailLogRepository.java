package com.esprit.notificationms.repository;

import com.esprit.notificationms.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByRecipient(String recipient);

    List<EmailLog> findByStatus(com.esprit.notificationms.enums.EmailStatus status);

    List<EmailLog> findByServiceOrigin(String serviceOrigin);
}
