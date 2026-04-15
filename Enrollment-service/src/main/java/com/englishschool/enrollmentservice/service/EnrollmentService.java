package com.englishschool.enrollmentservice.service;

import com.englishschool.enrollmentservice.client.CourseDTO;
import com.englishschool.enrollmentservice.client.CourseFeignClient;
import com.englishschool.enrollmentservice.dto.EnrollmentDTO;
import com.englishschool.enrollmentservice.dto.MyCourseDTO;
import com.englishschool.enrollmentservice.entity.Enrollment;
import com.englishschool.enrollmentservice.entity.IdempotencyKey;
import com.englishschool.enrollmentservice.entity.StudentProgress;
import com.englishschool.enrollmentservice.exception.DuplicateEnrollmentException;
import com.englishschool.enrollmentservice.exception.ResourceNotFoundException;
import com.englishschool.enrollmentservice.repository.CertificateRepository;
import com.englishschool.enrollmentservice.repository.EnrollmentRepository;
import com.englishschool.enrollmentservice.repository.IdempotencyKeyRepository;
import com.englishschool.enrollmentservice.repository.ProgressRepository;
import com.englishschool.enrollmentservice.repository.StudentProgressRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository repository;
    private final CertificateRepository certificateRepository;
    private final ProgressRepository progressRepository;
    private final StudentProgressRepository studentProgressRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final CourseFeignClient courseFeignClient;
    private final ProgressService progressService;
    private final ObjectMapper objectMapper;

    @Transactional
    public EnrollmentDTO enroll(EnrollmentDTO dto, String idempotencyKey) {
        String keyHash = idempotencyKey != null && !idempotencyKey.isBlank()
                ? hashKey(idempotencyKey) : null;
        if (keyHash != null) {
            var existing = idempotencyKeyRepository.findByKeyHash(keyHash);
            if (existing.isPresent() && existing.get().getResponseBody() != null) {
                try {
                    return objectMapper.readValue(existing.get().getResponseBody(), EnrollmentDTO.class);
                } catch (JsonProcessingException e) {
                    log.warn("Could not deserialize idempotent response: {}", e.getMessage());
                }
            }
        }

        Long userId = dto.getUserId() != null ? dto.getUserId() : 1L;
        var existingEnrollment = repository.findByUserIdAndCourseId(userId, dto.getCourseId());
        if (existingEnrollment.isPresent() && "active".equals(existingEnrollment.get().getStatus())) {
            EnrollmentDTO existingDto = toDTO(existingEnrollment.get());
            if (keyHash != null) {
                try {
                    idempotencyKeyRepository.save(IdempotencyKey.builder()
                            .keyHash(keyHash)
                            .responseBody(objectMapper.writeValueAsString(existingDto))
                            .build());
                } catch (Exception ignored) {}
            }
            return existingDto;
        }
        try {
            courseFeignClient.getCourseById(dto.getCourseId());
        } catch (Exception ex) {
            log.warn("Course validation skipped: {}", ex.getMessage());
        }
        Enrollment e = Enrollment.builder()
                .userId(userId)
                .studentName(dto.getStudentName())
                .courseId(dto.getCourseId())
                .status(dto.getStatus() != null ? dto.getStatus() : "active")
                .build();
        e = repository.save(e);
        EnrollmentDTO result = toDTO(e);
        if (keyHash != null) {
            try {
                idempotencyKeyRepository.save(IdempotencyKey.builder()
                        .keyHash(keyHash)
                        .responseBody(objectMapper.writeValueAsString(result))
                        .build());
            } catch (JsonProcessingException ex) {
                log.warn("Could not store idempotent response: {}", ex.getMessage());
            }
        }
        return result;
    }

    public List<EnrollmentDTO> getAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public EnrollmentDTO getById(Long id) {
        Enrollment e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
        return toDTO(e);
    }

    public List<MyCourseDTO> getMyCourses(Long userId) {
        return repository.findByUserIdOrderByEnrolledAtDesc(userId).stream()
                // Show all non-cancelled enrollments, regardless of exact status casing
                .filter(en -> {
                    String status = en.getStatus();
                    return status == null || !"cancelled".equalsIgnoreCase(status);
                })
                .map(this::toMyCourseDTO)
                .collect(Collectors.toList());
    }

    public List<EnrollmentDTO> getEnrollmentHistory(Long userId) {
        return repository.findByUserIdOrderByEnrolledAtDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<EnrollmentDTO> getByUserId(Long userId) {
        return repository.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public EnrollmentDTO cancel(Long id) {
        Enrollment e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
        e.setStatus("cancelled");
        return toDTO(repository.save(e));
    }

    @Transactional
    public EnrollmentDTO update(Long id, EnrollmentDTO dto) {
        Enrollment e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
        if (dto.getUserId() != null) e.setUserId(dto.getUserId());
        if (dto.getStudentName() != null) e.setStudentName(dto.getStudentName());
        if (dto.getCourseId() != null) e.setCourseId(dto.getCourseId());
        if (dto.getStatus() != null) e.setStatus(dto.getStatus());
        return toDTO(repository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Enrollment", id);
        // Delete dependent records first (foreign key constraints) - use bulk DELETE
        certificateRepository.deleteByEnrollmentId(id);
        progressRepository.deleteByEnrollmentId(id);
        studentProgressRepository.deleteByEnrollmentId(id);
        repository.deleteById(id);
    }

    private MyCourseDTO toMyCourseDTO(Enrollment e) {
        CourseDTO course = null;
        try {
            course = courseFeignClient.getCourseById(e.getCourseId());
        } catch (Exception ex) {
            log.debug("Could not fetch course {}: {}", e.getCourseId(), ex.getMessage());
        }
        int progress = 0;
        try {
            progress = progressService.getCourseProgressPercent(e.getId());
        } catch (Exception ex) {
            log.debug("Could not compute progress for enrollment {}: {}", e.getId(), ex.getMessage());
        }
        return MyCourseDTO.builder()
                .enrollmentId(e.getId())
                .courseId(e.getCourseId())
                .status(e.getStatus())
                .progressPercent(progress)
                .enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt())
                .course(course)
                .build();
    }

    private EnrollmentDTO toDTO(Enrollment e) {
        int progress = progressService.getCourseProgressPercentCached(e.getId());
        return EnrollmentDTO.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .studentName(e.getStudentName())
                .courseId(e.getCourseId())
                .status(e.getStatus())
                .enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt())
                .progressPercent(progress)
                .build();
    }

    private static String hashKey(String key) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return key;
        }
    }
}
