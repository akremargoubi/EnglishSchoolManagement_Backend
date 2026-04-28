package com.example.assessmentservice.repository;

import com.example.assessmentservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByReadFalseOrderByCreatedAtDesc();

    long countByReadFalse();

    // User-scoped: own notifications + broadcasts (targetEmail IS NULL)
    @Query("SELECT n FROM Notification n WHERE (n.targetEmail = :email OR n.targetEmail IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findForUser(@Param("email") String email);

    @Query("SELECT n FROM Notification n WHERE (n.targetEmail = :email OR n.targetEmail IS NULL) AND n.read = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadForUser(@Param("email") String email);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.targetEmail = :email OR n.targetEmail IS NULL) AND n.read = false")
    long countUnreadForUser(@Param("email") String email);
}
