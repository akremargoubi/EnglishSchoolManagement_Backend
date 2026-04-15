package tn.esprit.reportingservice.repository;

import tn.esprit.reportingservice.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
