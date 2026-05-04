package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.StudentClassRequest;
import com.esprit.esmauthms.dto.StudentClassResponseDto;
import com.esprit.esmauthms.dto.StudentClassResponseDto.StudentSummary;
import com.esprit.esmauthms.entity.StudentClass;
import com.esprit.esmauthms.entity.User;
import com.esprit.esmauthms.repository.StudentClassRepository;
import com.esprit.esmauthms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentClassService {

    private final StudentClassRepository studentClassRepository;
    private final UserRepository userRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public StudentClassResponseDto create(StudentClassRequest request) {
        if (studentClassRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Class name already exists: " + request.getName());
        }

        StudentClass.StudentClassBuilder builder = StudentClass.builder()
                .name(request.getName())
                .level(request.getLevel())
                .specialty(request.getSpecialty())
                .description(request.getDescription());

        if (request.getTutorId() != null) {
            User tutor = findUserOrThrow(request.getTutorId());
            builder.tutor(tutor);
        }

        return toDto(studentClassRepository.save(builder.build()));
    }

    @Transactional(readOnly = true)
    public List<StudentClassResponseDto> getAll() {
        return studentClassRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentClassResponseDto getById(Long id) {
        return toDto(findClassOrThrow(id));
    }

    @Transactional
    public StudentClassResponseDto update(Long id, StudentClassRequest request) {
        StudentClass c = findClassOrThrow(id);

        if (request.getName() != null)        c.setName(request.getName());
        if (request.getLevel() != null)       c.setLevel(request.getLevel());
        if (request.getSpecialty() != null)   c.setSpecialty(request.getSpecialty());
        if (request.getDescription() != null) c.setDescription(request.getDescription());

        // Allow updating the tutor inline too
        if (request.getTutorId() != null) {
            c.setTutor(findUserOrThrow(request.getTutorId()));
        }

        return toDto(studentClassRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        StudentClass c = findClassOrThrow(id);

        // Unlink all students before deletion to avoid FK constraint
        if (c.getStudents() != null) {
            c.getStudents().forEach(u -> {
                u.setStudentClass(null);
                userRepository.save(u);
            });
        }

        studentClassRepository.deleteById(id);
    }

    // ── Students ──────────────────────────────────────────────────────────────

    @Transactional
    public StudentClassResponseDto assignStudent(Long classId, UUID userId) {
        StudentClass c = findClassOrThrow(classId);
        User user = findUserOrThrow(userId);
        user.setStudentClass(c);
        userRepository.save(user);
        return toDto(studentClassRepository.findById(classId).orElseThrow());
    }

    @Transactional
    public StudentClassResponseDto removeStudent(Long classId, UUID userId) {
        findClassOrThrow(classId);
        User user = findUserOrThrow(userId);
        if (user.getStudentClass() == null || !classId.equals(user.getStudentClass().getId())) {
            throw new RuntimeException("User " + userId + " is not in class " + classId);
        }
        user.setStudentClass(null);
        userRepository.save(user);
        return toDto(studentClassRepository.findById(classId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<StudentSummary> getStudents(Long classId) {
        findClassOrThrow(classId);
        StudentClass c = studentClassRepository.findById(classId).orElseThrow();
        return toStudentSummaries(c.getStudents());
    }

    // ── Tutor ─────────────────────────────────────────────────────────────────

    @Transactional
    public StudentClassResponseDto assignTutor(Long classId, UUID tutorId) {
        StudentClass c = findClassOrThrow(classId);
        User tutor = findUserOrThrow(tutorId);
        c.setTutor(tutor);
        return toDto(studentClassRepository.save(c));
    }

    @Transactional
    public StudentClassResponseDto removeTutor(Long classId) {
        StudentClass c = findClassOrThrow(classId);
        c.setTutor(null);
        return toDto(studentClassRepository.save(c));
    }

    // ── Tutor lookup (used by downstream services for authorization) ──────────

    @Transactional(readOnly = true)
    public List<StudentClassResponseDto> getClassesByTutor(UUID tutorId) {
        return studentClassRepository.findByTutor_Id(tutorId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StudentClass findClassOrThrow(Long id) {
        return studentClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found: " + id));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    private List<StudentSummary> toStudentSummaries(List<User> students) {
        if (students == null) return List.of();
        return students.stream()
                .map(u -> StudentSummary.builder()
                        .id(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .cin(u.getCin())
                        .avatarUrl(u.getAvatarUrl())
                        .build())
                .collect(Collectors.toList());
    }

    StudentClassResponseDto toDto(StudentClass c) {
        StudentClassResponseDto.StudentClassResponseDtoBuilder builder = StudentClassResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .level(c.getLevel())
                .specialty(c.getSpecialty())
                .description(c.getDescription())
                .createdAt(c.getCreatedAt())
                .studentCount(c.getStudents() != null ? c.getStudents().size() : 0)
                .students(toStudentSummaries(c.getStudents()));

        if (c.getTutor() != null) {
            builder.tutorId(c.getTutor().getId())
                   .tutorFirstName(c.getTutor().getFirstName())
                   .tutorLastName(c.getTutor().getLastName())
                   .tutorEmail(c.getTutor().getEmail());
        }

        return builder.build();
    }
}
