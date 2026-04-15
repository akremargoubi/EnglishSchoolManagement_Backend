package com.englishschool.enrollmentservice.repository;

import com.englishschool.enrollmentservice.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByUserId(Long userId);
    java.util.Optional<Certificate> findByEnrollment_Id(Long enrollmentId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Certificate c WHERE c.enrollment.id = :enrollmentId")
    void deleteByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
