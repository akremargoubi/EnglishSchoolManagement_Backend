package com.example.assessmentservice.service;

import com.example.assessmentservice.client.AuthServiceClient;
import com.example.assessmentservice.entity.Assessment;
import com.example.assessmentservice.repository.AssessmentRepository;
import com.example.assessmentservice.security.CallerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository repository;
    private final AuthServiceClient authClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public Assessment create(Assessment assessment, CallerContext caller) {
        if (!caller.isAdmin() && !caller.isTutor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only tutors or admins can create assessments");
        }
        assessment.setTutorId(caller.userId());
        return repository.save(assessment);
    }

    public List<Assessment> getAll(CallerContext caller) {
        if (caller.isAdmin()) {
            return repository.findAll();
        }
        if (caller.isTutor()) {
            List<String> myClasses = authClient.getTutorClassNames(caller.userId());
            if (!myClasses.isEmpty()) {
                return repository.findByClassNameIn(myClasses);
            }
            // Fallback: return all assessments created by this tutor
            return repository.findByTutorId(caller.userId());
        }
        // Students should not use getAll — they use getByClassName
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    public Assessment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found: " + id));
    }

    public List<Assessment> getByClassName(String className, CallerContext caller) {
        if (caller.isAdmin()) {
            return repository.findByClassName(className);
        }
        if (caller.isTutor()) {
            if (!authClient.isTutorOfClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not the tutor of class: " + className);
            }
            return repository.findByClassName(className);
        }
        if (caller.isStudent()) {
            if (!authClient.isStudentInClass(caller.userId(), className)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not enrolled in class: " + className);
            }
            return repository.findByClassName(className);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    public Assessment update(Long id, Assessment updated, CallerContext caller) {
        Assessment existing = getById(id);
        if (!caller.isAdmin() && !caller.userId().equals(existing.getTutorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own assessments");
        }
        existing.setTitle(updated.getTitle());
        existing.setCourseName(updated.getCourseName());
        existing.setType(updated.getType());
        existing.setStatus(updated.getStatus());
        existing.setClassName(updated.getClassName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setDuration(updated.getDuration());
        return repository.save(existing);
    }

    public void delete(Long id, CallerContext caller) {
        Assessment existing = getById(id);
        if (!caller.isAdmin() && !caller.userId().equals(existing.getTutorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own assessments");
        }
        repository.deleteById(id);
    }

    // ── Planning (fixed broken methods) ────────────────────────────────────────

    public List<Assessment> getByMonth(int year, int month) {
        String prefix = String.format("%04d-%02d", year, month);
        return repository.findByYearMonth(prefix);
    }

    public List<Assessment> getUpcoming() {
        return repository.findUpcoming(LocalDate.now().format(DATE_FMT));
    }

    public List<Assessment> getOngoing() {
        return repository.findOngoing(LocalDate.now().format(DATE_FMT));
    }

    public List<Assessment> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByDateRange(
                start.toLocalDate().format(DATE_FMT),
                end.toLocalDate().format(DATE_FMT));
    }
}
