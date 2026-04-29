package com.esprit.notificationms.repository;

import com.esprit.notificationms.entity.ScheduledEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledEmailRepository extends JpaRepository<ScheduledEmail, Long> {

    List<ScheduledEmail> findByStatus(com.esprit.notificationms.enums.EmailStatus status);

    List<ScheduledEmail> findByStatusAndScheduledAtBefore(com.esprit.notificationms.enums.EmailStatus status, LocalDateTime time);

    List<ScheduledEmail> findByRecipient(String recipient);
}
