package tn.esprit.reportingservice.service;

import tn.esprit.reportingservice.entity.Report;
import tn.esprit.reportingservice.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final ReportRepository repository;

    public ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    public Report create(Report r) {
        LocalDateTime now = LocalDateTime.now();
        if (r.getStatus() == null || r.getStatus().isBlank()) {
            r.setStatus("IN_PROGRESS");
        }
        r.setCreatedDate(now);
        r.setUpdatedDate(now);
        return repository.save(r);
    }

    public List<Report> getAll() {
        return repository.findAll();
    }

    public Report getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Report update(Long id, Report r) {
        Report existing = repository.findById(id).orElseThrow();
        existing.setSubject(r.getSubject());
        existing.setMessage(r.getMessage());
        existing.setStudentEmail(r.getStudentEmail());
        existing.setCategory(r.getCategory());
        existing.setPriority(r.getPriority());
        if (r.getStatus() != null && !r.getStatus().isBlank()) {
            existing.setStatus(r.getStatus());
        }
        existing.setUpdatedDate(LocalDateTime.now());
        return repository.save(existing);
    }

    public Report updateStatus(Long id, String status) {
        Report existing = repository.findById(id).orElseThrow();
        if (!"IN_PROGRESS".equals(existing.getStatus())) {
            return existing;
        }
        existing.setStatus(status);
        existing.setUpdatedDate(LocalDateTime.now());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
