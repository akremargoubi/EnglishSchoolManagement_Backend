package com.englishschool.enrollmentservice.service;

import com.englishschool.enrollmentservice.client.CourseFeignClient;
import com.englishschool.enrollmentservice.client.LessonDTO;
import com.englishschool.enrollmentservice.client.ModuleDTO;
import com.englishschool.enrollmentservice.dto.ProgressDTO;
import com.englishschool.enrollmentservice.entity.Enrollment;
import com.englishschool.enrollmentservice.entity.Progress;
import com.englishschool.enrollmentservice.entity.StudentProgress;
import com.englishschool.enrollmentservice.exception.ResourceNotFoundException;
import com.englishschool.enrollmentservice.repository.EnrollmentRepository;
import com.englishschool.enrollmentservice.repository.ProgressRepository;
import com.englishschool.enrollmentservice.repository.StudentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final StudentProgressRepository studentProgressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseFeignClient courseFeignClient;

    @Transactional
    public ProgressDTO markLessonComplete(Long enrollmentId, Long lessonId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));
        String status = enrollment.getStatus() != null ? enrollment.getStatus().toLowerCase() : "";
        if (!"active".equals(status) && !"completed".equals(status)) {
            throw new IllegalStateException("Cannot update progress for non-active enrollment");
        }
        if (progressRepository.existsByEnrollment_IdAndLessonId(enrollmentId, lessonId)) {
            return progressRepository.findByEnrollment_Id(enrollmentId).stream()
                    .filter(p -> p.getLessonId().equals(lessonId))
                    .findFirst()
                    .map(this::toDTO)
                    .orElseThrow();
        }
        Progress p = Progress.builder()
                .enrollment(enrollment)
                .lessonId(lessonId)
                .build();
        p = progressRepository.save(p);
        recalculateStudentProgress(enrollmentId);
        return toDTO(p);
    }

    public List<ProgressDTO> getLessonProgress(Long enrollmentId) {
        return progressRepository.findByEnrollment_Id(enrollmentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public int getCourseProgressPercent(Long enrollmentId) {
        return studentProgressRepository.findByEnrollment_Id(enrollmentId)
                .map(StudentProgress::getProgressPercent)
                .orElseGet(() -> computeAndSaveProgress(enrollmentId));
    }

    /** Lightweight read - returns 0 if no StudentProgress yet (avoids Feign/compute). */
    public int getCourseProgressPercentCached(Long enrollmentId) {
        return studentProgressRepository.findByEnrollment_Id(enrollmentId)
                .map(StudentProgress::getProgressPercent)
                .orElse(0);
    }

    private void recalculateStudentProgress(Long enrollmentId) {
        computeAndSaveProgress(enrollmentId);
    }

    private int computeAndSaveProgress(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));
        int totalLessons = 0;
        try {
            List<ModuleDTO> modules = courseFeignClient.getModulesByCourseId(enrollment.getCourseId(), true);
            for (ModuleDTO m : modules) {
                if (m.getLessons() != null) totalLessons += m.getLessons().size();
            }
        } catch (Exception ignored) {
            totalLessons = 1;
        }
        int completedLessons = progressRepository.findByEnrollment_Id(enrollmentId).size();
        int percent = totalLessons > 0 ? (completedLessons * 100) / totalLessons : 0;

        StudentProgress sp = studentProgressRepository.findByEnrollment_Id(enrollmentId)
                .orElseGet(() -> StudentProgress.builder().enrollment(enrollment).build());
        sp.setTotalLessons(totalLessons);
        sp.setCompletedLessons(completedLessons);
        sp.setProgressPercent(percent);
        studentProgressRepository.save(sp);

        if (percent == 100) {
            enrollment.setCompletedAt(Instant.now());
            enrollment.setStatus("completed");
            enrollmentRepository.save(enrollment);
        }
        return percent;
    }

    private ProgressDTO toDTO(Progress p) {
        return ProgressDTO.builder()
                .id(p.getId())
                .enrollmentId(p.getEnrollment().getId())
                .lessonId(p.getLessonId())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
