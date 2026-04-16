package com.esprit.esmauthms.service;

import com.esprit.esmauthms.dto.StudentClassRequest;
import com.esprit.esmauthms.dto.StudentClassResponseDto;
import com.esprit.esmauthms.entity.StudentClass;
import com.esprit.esmauthms.entity.User;
import com.esprit.esmauthms.repository.StudentClassRepository;
import com.esprit.esmauthms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentClassService {

    private final StudentClassRepository studentClassRepository;
    private final UserRepository userRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public StudentClassResponseDto create(StudentClassRequest request) {
        if (studentClassRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("StudentClass name already exists: " + request.getName());
        }
        StudentClass c = StudentClass.builder()
                .name(request.getName())
                .level(request.getLevel())
                .specialty(request.getSpecialty())
                .description(request.getDescription())
                .build();
        return toDto(studentClassRepository.save(c));
    }

    public List<StudentClassResponseDto> getAll() {
        return studentClassRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public StudentClassResponseDto getById(Long id) {
        return toDto(findById(id));
    }

    public StudentClassResponseDto update(Long id, StudentClassRequest request) {
        StudentClass c = findById(id);
        if (request.getName() != null)        c.setName(request.getName());
        if (request.getLevel() != null)       c.setLevel(request.getLevel());
        if (request.getSpecialty() != null)   c.setSpecialty(request.getSpecialty());
        if (request.getDescription() != null) c.setDescription(request.getDescription());
        return toDto(studentClassRepository.save(c));
    }

    public void delete(Long id) {
        studentClassRepository.deleteById(id);
    }

    // ── Assigner / désassigner un étudiant ────────────────────────────────────

    /**
     * Assigne un étudiant à une classe.
     * PUT /api/classes/{classId}/students/{userId}
     */
    public void assignStudent(Long classId, UUID userId) {
        StudentClass c = findById(classId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setStudentClass(c);
        userRepository.save(user);
    }

    /**
     * Retire un étudiant d'une classe.
     * DELETE /api/classes/{classId}/students/{userId}
     */
    public void removeStudent(Long classId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setStudentClass(null);
        userRepository.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StudentClass findById(Long id) {
        return studentClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StudentClass not found: " + id));
    }

    private StudentClassResponseDto toDto(StudentClass c) {
        return StudentClassResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .level(c.getLevel())
                .specialty(c.getSpecialty())
                .description(c.getDescription())
                .createdAt(c.getCreatedAt())
                .studentCount(c.getStudents() != null ? c.getStudents().size() : 0)
                .build();
    }
}